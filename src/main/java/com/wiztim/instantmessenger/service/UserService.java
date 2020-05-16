package com.wiztim.instantmessenger.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.wiztim.instantmessenger.dto.MessageWrapperDTO;
import com.wiztim.instantmessenger.dto.UserInfoDTO;
import com.wiztim.instantmessenger.dto.UserDetailsDTO;
import com.wiztim.instantmessenger.dto.UserProfileDTO;
import com.wiztim.instantmessenger.enums.MessageCategory;
import com.wiztim.instantmessenger.exceptions.NullIdException;
import com.wiztim.instantmessenger.exceptions.UserNotFoundException;
import com.wiztim.instantmessenger.persistence.user.UserDetails;
import com.wiztim.instantmessenger.dto.UserStatusDTO;
import com.wiztim.instantmessenger.enums.Status;
import com.wiztim.instantmessenger.exceptions.DuplicateEntityException;
import com.wiztim.instantmessenger.exceptions.DisabledEntityException;
import com.wiztim.instantmessenger.exceptions.InvalidEntityException;
import com.wiztim.instantmessenger.exceptions.UserAlreadyEnabledException;
import com.wiztim.instantmessenger.exceptions.UserDisabledException;
import com.wiztim.instantmessenger.persistence.user.User;
import com.wiztim.instantmessenger.persistence.user.UserSession;
import com.wiztim.instantmessenger.persistence.user.UserSettings;
import com.wiztim.instantmessenger.persistence.user.UserStatus;
import com.wiztim.instantmessenger.repository.UserRepository;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.Session;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
@Setter
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionService sessionService;

    private final LoadingCache<UUID, UserInfoDTO> userInfoCache = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public UserInfoDTO load(UUID id) {
            User user = userRepository.get(id);
            if (user == null) {
                log.warn("userInfoCache failed to load user with id: {}", id);
                return null;
            }

            if (!user.getUserDetails().isEnabled()) {
                log.info("userInfoCache attempted to load deactivated user with id: {}", id);
                return null;
            }

            return User.toUserDTO(user, UserStatus.offlineNow());
        }
    });

    public void newUserSession(UUID id, Session session, Status status) {
        validateUserEnabled(id);

        if (session == null) {
            log.warn("User {} attempted to create new session, but session was null", id);
            throw new InvalidEntityException();
        }

        validateSessionId(session.getId());

        if (status == null || Status.Offline.equals(status)) {
            log.warn("User {} attempted to create new session with a bad status: {}", id, status);
            throw new InvalidEntityException();
        }

        UserStatus userStatus = UserStatus.builder()
                .status(status)
                .time(Instant.now().toEpochMilli())
                .build();

        UserSession userSession = UserSession.builder()
                .session(session)
                .userStatus(userStatus)
                .build();

        sessionService.addSession(id, userSession);
        setStatus(id, session.getId(), status);
    }

    public void endUserSession(Session session) {
        UUID id = sessionService.closeSession(session.getId());
        UserStatus latestSessionStatus = sessionService.getMostRecentUserStatus(id);
        if (latestSessionStatus == null) {
            setStatus(id, session.getId(), Status.Offline);
        } else {
            setStatus(id, session.getId(), latestSessionStatus.getStatus());
        }
    }

    public User createUser(String username) {
        UserDetails userDetails = UserDetails.builder()
                .usernames(Set.of(username))
                .build();

        return createUser(userDetails);
    }

    public User createUser(UserDetails userDetails) {
        log.info("Creating new user {}", userDetails);
        validateUserDetails(userDetails);
        validateUsernames(userDetails.getUsernames());

        // check if user with username already exists
        for (String username : userDetails.getUsernames()) {
            if (userRepository.isExistingUsername(username)) {
                log.warn("Failed to create new user, user with username {} already exists", username);
                throw new DuplicateEntityException(username);
            }
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .userDetails(userDetails)
                .userSettings(UserSettings.defaultSettings())
                .build();

        userRepository.createUser(user);

        // update the cache, we have a new user!
        userInfoCache.put(user.getId(), User.toUserDTO(user));

        // let clients know a new user has been made
        sessionService.sendMessageToAll(new MessageWrapperDTO(MessageCategory.NewUser, user));
        log.info("New user created {}", user);
        return user;
    }

    public User getUserByUsername(String username) {
        validateUsername(username);
        return userRepository.getByUsername(username);
    }

    // When a user logs in, they need the entire list of enabled users with their status
    // We trust that the cache is accurate and up to date
    public Collection<UserInfoDTO> getAllUserInfo() {
        // TODO page results, don't want to send too many users in one call
        return userInfoCache.asMap().values();
    }

    public UserInfoDTO getUserInfoByUsername(String username) {
        validateUsername(username);

        User user = userRepository.getByUsername(username);
        if (user == null) {
            log.warn("getUserInfoByUsername failed to find User for username: \"{}\"", username);
            throw new InvalidEntityException();
        }

        if (!user.getUserDetails().isEnabled()) {
            log.warn("getUserInfoByUsername attempted to get user info for disabled username: \"{}\"", username);
            throw new UserDisabledException(user.getId());
        }

        return getUserInfo(user.getId());
    }

    public UserInfoDTO getUserInfo(UUID id) {
        return userInfoCache.getUnchecked(id);
    }

    public void updateUserProfile(UserProfileDTO userProfileDTO) {
        validateUserProfile(userProfileDTO);

        UUID id = userProfileDTO.getId();
        User user = getExistingUser(id);
        UserSettings userSettings = userProfileDTO.getUserSettings();
        UserDetails userDetails = userProfileDTO.getUserDetails();

        // we don't allow changing enabled / disabled or associated usernames through this call
        userDetails.setEnabled(user.getUserDetails().isEnabled());
        userDetails.setUsernames(user.getUserDetails().getUsernames());

        boolean userDetailsChanged = !userDetails.equals(user.getUserDetails());
        boolean userSettingsChanged = !userSettings.equals(user.getUserSettings());

        if (!userDetailsChanged && !userSettingsChanged) {
            log.info("updateUserProfile was called with no changes made to the user");
            return;
        }

        user.setUserDetails(userDetails);
        user.setUserSettings(userSettings);

        userRepository.updateUser(user);

        // let everyone know that a user details has changed
        if (userDetailsChanged) {
            log.info("User details have changed, broadcasting new details {}", userDetails);
            userInfoCache.getUnchecked(id).setUserDetails(userDetails);

            UserDetailsDTO userDetailsDTO = new UserDetailsDTO(id, userDetails);
            sessionService.sendMessageToAll(new MessageWrapperDTO(MessageCategory.UpdateUserDetails, userDetailsDTO));
        }
    }

    public void setStatus(UUID id, String sessionId, Status status) {
        validateId(id);

        UserStatus userStatus = UserStatus.builder()
                .status(status)
                .time(Instant.now().toEpochMilli())
                .build();

        setStatus(id, sessionId, userStatus);
    }

    private void setStatus(UUID id, String sessionId, UserStatus userStatus) {
        log.debug("setStatus called; id: {} sessionId: {} userStatus: {}", id, sessionId, userStatus);
        validateId(id);
        validateSessionId(sessionId);
        validateUserStatus(userStatus);

        Status status = userStatus.getStatus();

        if (Status.Offline.equals(status)) {
            log.info("setStatus received offline status, closing user session userId {} sessionId {}", id, sessionId);
            sessionService.closeSession(sessionId);
        } else {
            // success means that the session exists
            boolean success = sessionService.updateSessionStatus(sessionId, userStatus);
            if (!success) {
                log.warn("setStatus tried to update status for a session that doesn't exist. userId {} sessionId {}", id, sessionId);
                // if this session does not exist, it must be offline
                userStatus.setStatus(Status.Offline);
            }
        }

        // getMostRecentUserStatus will prioritize all other statuses above ComputerLocked
        UserStatus latestSessionStatus = sessionService.getMostRecentUserStatus(id);

        // get current userDto / status
        UserInfoDTO userInfo = userInfoCache.getUnchecked(id);
        if (userInfo == null) {
            log.warn("setStatus failed to get userInfo for user {}", id);
            throw new UserNotFoundException(id);
        }

        UserStatus currentStatus = userInfo.getUserStatus();
        if (latestSessionStatus != null) {
            if (!currentStatus.getStatus().equals(latestSessionStatus.getStatus())) {
                // If the new status is the same as the current status, we don't need to do anything else
                log.debug("setStatus received, but status hasn't changed, terminating early");
                return;
            } else {
                log.error("setStatus received, and the latest status isn't equal to the current status. Correcting current status. " +
                        "If this happens, this is a bug, because the current status should always be correct.");
                userStatus.setStatus(latestSessionStatus.getStatus());
            }
        }

        // publish this new status to all online clients
        UserStatusDTO userStatusDTO = UserStatus.toUserStatusDTO(id, userStatus);
        userInfo.setUserStatus(userStatus);
        userInfoCache.put(id, userInfo);

        // by the end of this call, if the status is offline, then it means the service believes the user is offline and has no active sessions
        // just in case we messed up somewhere and there are sessions still in the cache, we should clean then up
        // if there actually any valid and active sessions, the client will receive a message letting them know we closed their session
        Status finalStatus = userStatus.getStatus();
        if (Status.Offline.equals(finalStatus)) {
            sessionService.closeUserSessions(id);
        }

        if (!userInfo.getUserDetails().isEnabled()) {
            log.warn("setStatus called on a deactivated user: {}", userInfo);
            // we don't store deactivated users in the cache
            userInfoCache.invalidate(id);
            throw new UserDisabledException(id);
        }

        sessionService.sendMessageToAllExceptSelf(id, new MessageWrapperDTO(MessageCategory.UpdateUserStatus, userStatusDTO));

        log.debug("setStatus finished; id: {} sessionId: {} userStatus: {}", id, sessionId, userStatus);
    }

    public void enableUser(UUID id) {
        log.info("enabling user {}", id);
        validateId(id);

        User user = getExistingUser(id);
        if (!user.getUserDetails().isEnabled()) {
            log.warn("Enable user called on an already enabled user {}", id);
            throw new UserAlreadyEnabledException(id);
        }

        user.getUserDetails().setEnabled(true);
        userRepository.updateUser(user);

        // add user to the cache
        UserInfoDTO userInfoDTO = User.toUserDTO(user);
        userInfoCache.put(id, userInfoDTO);

        sessionService.sendMessageToAll(new MessageWrapperDTO(MessageCategory.NewUser, user));
        log.info("successfully enabled user {}", id);
    }

    public void disableUser(UUID id) {
        log.info("disabling user {}", id);
        validateId(id);

        User user = getExistingUser(id);
        if (!user.getUserDetails().isEnabled()) {
            log.warn("Disable user called on an already Disabled user {}", id);
            throw new UserDisabledException(id);
        }

        user.getUserDetails().setEnabled(false);
        userRepository.updateUser(user);

        // remove user from cache
        userInfoCache.invalidate(id);
        sessionService.closeUserSessions(id);

        sessionService.sendMessageToAll(new MessageWrapperDTO(MessageCategory.DisableUser, id));
        log.info("successfully disabled user {}", id);
    }

    // takes a list of userIds and returns which of those users are currently enabled
    protected Set<UUID> getEnabledUserIds(Set<UUID> userIds) {
        Set<UUID> enabledUserIds = new HashSet<>();

        for (UUID id : userIds) {
            UserInfoDTO userInfo = userInfoCache.getUnchecked(id);
            if (userInfo != null && userInfo.getUserDetails().isEnabled()) {
                enabledUserIds.add(id);
            }
        }

        return enabledUserIds;
    }

    public User getExistingUser(UUID id) {
        validateId(id);

        User user = userRepository.get(id);
        if (user == null) {
            log.warn("getExistingUser could not find user with id: {}", id);
            throw new UserNotFoundException(id);
        }

        return user;
    }

    public String getFullname(UUID id) {
        log.info("getting fullname for user {}", id);
        UserInfoDTO userInfo = userInfoCache.getUnchecked(id);
        // We don't keep deactivated users in the cache
        if (userInfo == null) {
            User user = getExistingUser(id);
            return user.getUserDetails().getFullname();
        } else {
            return userInfo.getUserDetails().getFullname();
        }
    }

    protected void validateUserEnabled(UUID id) {
        validateId(id);

        User user = getExistingUser(id);
        if (!user.getUserDetails().isEnabled()) {
            log.warn("validateUserEnabled exception, user {} is disabled", id);
            throw new DisabledEntityException(id);
        }
    }

    private void validateId(UUID id) {
        if (id == null ) {
            log.warn("User Id failed validation: null");
            throw new NullIdException();
        }
    }

    private void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("User failed sessionId validation: {}", sessionId);
            throw new NullIdException();
        }
    }

    private void validateUsernames(Set<String> usernames) {
        log.debug("Validating usernames: {}", usernames);

        if (usernames == null || usernames.size() == 0) {
            log.warn("Username failed validation, null or empty");
            throw new InvalidEntityException();
        }

        for (String username : usernames) {
            validateUsername(username);
        }
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            log.warn("Username {} failed validation", username);
            throw new InvalidEntityException();
        }
    }

    private void validateUserProfile(UserProfileDTO userProfile) {
        if (userProfile == null || userProfile.getUserDetails() == null || userProfile.getUserSettings() == null) {
            log.warn("User info failed validation: " + userProfile);
            throw new InvalidEntityException();
        }

        validateId(userProfile.getId());
        validateUserDetails(userProfile.getUserDetails());
        validateUserSettings(userProfile.getUserSettings());
    }

    private void validateUserDetails(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsernames() == null || userDetails.getUsernames().size() == 0) {
            log.warn("User info failed validation: " + userDetails);
            throw new InvalidEntityException();
        }

        validateUsernames(userDetails.getUsernames());
    }

    private void validateUserSettings(UserSettings userSettings) {
        // TODO max font size should be a dynamic server config
        if (userSettings == null || userSettings.getFontSize() <= 0 || userSettings.getFontSize() > 200) {
            log.warn("User settings failed validation: " + userSettings);
            throw new InvalidEntityException();
        }
    }

    private void validateUserStatus(UserStatus userStatus) {
        if (userStatus == null || userStatus.getStatus() == null || userStatus.getTime() <= 0) {
            log.warn("User status failed validation: " + userStatus);
            throw new InvalidEntityException();
        }
    }
}
