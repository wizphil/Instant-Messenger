package com.wiztim.instantmessenger.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.wiztim.instantmessenger.dto.MessageWrapperDTO;
import com.wiztim.instantmessenger.dto.UserInfoDTO;
import com.wiztim.instantmessenger.dto.UserDetailsDTO;
import com.wiztim.instantmessenger.dto.UserProfileDTO;
import com.wiztim.instantmessenger.enums.MessageCategory;
import com.wiztim.instantmessenger.exceptions.SessionNotFoundException;
import com.wiztim.instantmessenger.exceptions.UserNotFoundException;
import com.wiztim.instantmessenger.persistence.user.UserDetails;
import com.wiztim.instantmessenger.dto.UserStatusDTO;
import com.wiztim.instantmessenger.enums.Status;
import com.wiztim.instantmessenger.exceptions.DuplicateEntityException;
import com.wiztim.instantmessenger.exceptions.DisabledEntityException;
import com.wiztim.instantmessenger.exceptions.InvalidEntityException;
import com.wiztim.instantmessenger.exceptions.RepositoryException;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
            User user = getUser(id);
            if (user == null) {
                return null;
            }

            return User.toUserDTO(user, UserStatus.offlineNow());
        }
    });

    private final LoadingCache<String, UUID> usernameToId = CacheBuilder.newBuilder().build(new CacheLoader<String, UUID>() {
        @Override
        public UUID load(String s) {
            return null;
        }
    });

    public void newUserSession(UUID id, Session session, Status status) {
        // TODO throw exception when status is Offline
        UserStatus userStatus = UserStatus.builder()
                .status(status)
                .time(Instant.now().toEpochMilli())
                .build();

        UserSession userSession = UserSession.builder()
                .session(session)
                .userStatus(userStatus)
                .build();

        sessionService.addSession(id, userSession);
        setUserStatus(id, session.getId(), status);
    }

    public void endUserSession(Session session) {
        UUID id = sessionService.removeSession(session.getId());
        setUserStatus(id, session.getId(), Status.Offline);
    }

    public User createUser(String username) {
        UserDetails userDetails = UserDetails.builder()
                .username(username)
                .build();

        return createUser(userDetails);
    }

    public User createUser(UserDetails userDetails) {
        validateUserDetails(userDetails);
        if (userDetails.getFullname() == null) {
            userDetails.setFullname(userDetails.getUsername());
        }
        if (userDetails.getExtension() == null) {
            userDetails.setExtension("");
        }

        // check if user with username already exists
        if (userRepository.isExistingUsername(userDetails.getUsername())) {
            throw new DuplicateEntityException(userDetails.getUsername());
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .userDetails(userDetails)
                .userSettings(UserSettings.defaultSettings())
                .build();

        // surely this can't happen
        if (userRepository.userExists(user.getId())) {
            throw new DuplicateEntityException(user.getId());
        }

        try {
            userRepository.createUser(user);
        } catch (Exception e) {
            throw new RepositoryException(e);
        }

        // update the cache, we have a new user!
        userInfoCache.put(user.getId(), User.toUserDTO(user));

        // let clients know a new user has been made
        sessionService.sendMessageToAll(new MessageWrapperDTO(MessageCategory.NewUser, user));
        return user;
    }

    public User getUser(UUID id) {
        if (id == null) {
            return null;
        }

        return userRepository.get(id);
    }

    public User getUserByUsername(String username) {
        UUID id = usernameToId.getUnchecked(username);
        if (id == null) {
            return null;
        }

        return getUser(id);
    }

    // When a user logs in, they need the entire list of enabled users with their status
    // We trust that the cache is accurate and up to date
    public Collection<UserInfoDTO> getAllUserInfo() {
        // TODO page results, don't want to see too many users in one call
        return userInfoCache.asMap().values();
    }

    public UserInfoDTO getUserInfoByUsername(String username) {
        UUID id = usernameToId.getUnchecked(username);
        if (id == null) {
            return null;
        }

        return getUserInfo(id);
    }

    public UserInfoDTO getUserInfo(UUID id) {
        return userInfoCache.getUnchecked(id);
    }

    public void updateUserProfile(UserProfileDTO userProfileDTO) {
        UUID id = userProfileDTO.getId();

        UserDetails userDetails = userProfileDTO.getUserDetails();
        UserSettings userSettings = userProfileDTO.getUserSettings();

        validateUserExists(id);
        validateUserDetails(userDetails);
        validateUserSettings(userSettings);

        User user = getUser(id);
        if (user == null) {
            throw new UserNotFoundException(id);
        }
        // we don't allow changing enabled / disabled through this call
        userDetails.setEnabled(user.getUserDetails().isEnabled());

        boolean userDetailsChanged = !userDetails.equals(user.getUserDetails());
        boolean userSettingsChanged = !userSettings.equals(user.getUserSettings());

        if (!userDetailsChanged && !userSettingsChanged) {
            return;
        }

        user.setUserDetails(userDetails);
        user.setUserSettings(userSettings);

        updateUser(user);

        // let everyone know that a user details has changed
        if (userDetailsChanged) {
            userInfoCache.getUnchecked(id).setUserDetails(userDetails);

            UserDetailsDTO userDetailsDTO = new UserDetailsDTO(id, userDetails);
            sessionService.sendMessageToAll(new MessageWrapperDTO(MessageCategory.UpdateUserDetails, userDetailsDTO));
        }
    }

    public void setUserStatus(UUID id, String sessionId, Status status) {
        UserStatus userStatus = UserStatus.builder()
                .status(status)
                .time(Instant.now().toEpochMilli())
                .build();

        setUserStatus(id, sessionId, userStatus);
    }

    private void setUserStatus(UUID id, String sessionId, UserStatus userStatus) {
        // validate request
        if (userStatus == null) {
            throw new InvalidEntityException();
        }

        validateEnabledUser(id);

        // get current userDto / status
        UserInfoDTO userInfo = userInfoCache.getUnchecked(id);
        if (userInfo == null) {
            throw new UserNotFoundException(id);
        }

        Status status = userStatus.getStatus();

        if (Status.Offline.equals(status)) {
            sessionService.removeSession(sessionId);
        } else {
            boolean success = sessionService.updateSessionStatus(sessionId, userStatus);
            if (!success) {
                log.error("Attempted to update user status, but sessionId was not found. This should only happen when done through swagger / backend. userId: {}, sessionId: {}", id, sessionId);
                sessionService.removeSession(sessionId);

                // if this was the only session, mark them as offline
                if (sessionService.getSessionCount(id) == 0) {
                    setUserStatus(id, sessionId, Status.Offline);
                }

                throw new SessionNotFoundException(sessionId);
            }
        }

        // if this session is ComputerLocked, but there's a session that's not ComputerLocked, we don't update the status
        if (Status.ComputerLocked.equals(status)) {
            UserStatus latestSessionStatus = sessionService.getMostRecentUserStatus(id);
            if (!Status.ComputerLocked.equals(latestSessionStatus.getStatus())) {
                return;
            }
        }

        UserStatusDTO userStatusDTO = UserStatus.toUserStatusDTO(id, userStatus);
        userInfo.setUserStatus(userStatus);
        userInfoCache.put(id, userInfo);

        sessionService.sendMessageToAll(new MessageWrapperDTO(MessageCategory.UpdateUserStatus, userStatusDTO));
    }

    public void enableUser(UUID id) {
        User user = validateAndGetDisabledUser(id);
        user.getUserDetails().setEnabled(true);
        updateUser(user);

        // add user to the cache
        UserInfoDTO userInfoDTO = User.toUserDTO(user);
        userInfoCache.put(id, userInfoDTO);

        sessionService.sendMessageToAll(new MessageWrapperDTO(MessageCategory.NewUser, user));
    }

    public void disableUser(UUID id) {
        User user = validateAndGetEnabledUser(id);
        if (!user.getUserDetails().isEnabled()) {
            throw new UserDisabledException(id);
        }

        user.getUserDetails().setEnabled(false);
        updateUser(user);

        // remove user from cache
        userInfoCache.invalidate(id);
        sessionService.removeUserSessions(id);

        sessionService.sendMessageToAll(new MessageWrapperDTO(MessageCategory.DisableUser, id));
    }

    // takes a list of userIds and returns which of those users are currently online
    protected List<UUID> getOnlineUserIds(List<UUID> userIds) {
        return userInfoCache.asMap().values()
                .stream()
                .filter(user -> !Status.Offline.equals(user.getUserStatus().getStatus()))
                .map(UserInfoDTO::getId)
                .collect(Collectors.toList());
    }

    // takes a list of userIds and returns which of those users are currently enabled
    protected List<UUID> getEnabled(List<UUID> userIds) {
        return userInfoCache.asMap().values()
                .stream()
                .filter(userInfoDTO -> userInfoDTO.getUserDetails().isEnabled())
                .map(UserInfoDTO::getId)
                .collect(Collectors.toList());
    }

    public boolean isUserOffline(UUID id) {
        UserInfoDTO userInfo = userInfoCache.getUnchecked(id);
        if (id == null) {
            return true;
        }

        return Status.Offline.equals(userInfo.getUserStatus().getStatus());
    }

    public boolean isUserDisabled(String username) {
        UUID id = usernameToId.getUnchecked(username);
        UserInfoDTO userInfo = userInfoCache.getUnchecked(id);
        return userInfo == null || !userInfo.getUserDetails().isEnabled();
    }

    protected void validateUserExists(UUID id) {
        if (id == null || !userRepository.userExists(id)) {
            throw new InvalidEntityException();
        }
    }

    protected void validateEnabledUser(UUID id) {
        validateUserExists(id);
        User user = userRepository.get(id);
        if (!user.getUserDetails().isEnabled()) {
            throw new DisabledEntityException(id);
        }
    }

    protected User validateAndGetEnabledUser(UUID id) {
        validateUserExists(id);
        User user = userRepository.get(id);
        if (!user.getUserDetails().isEnabled()) {
            throw new DisabledEntityException(id);
        }

        return user;
    }

    protected User validateAndGetDisabledUser(UUID id) {
        validateUserExists(id);
        User user = userRepository.get(id);
        if (user.getUserDetails().isEnabled()) {
            throw new UserAlreadyEnabledException(id);
        }

        return user;
    }

    private void updateUser(User user) {
        try {
            userRepository.updateUser(user);
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    private void validateUserDetails(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            log.error("User info failed validation: " + userDetails);
            throw new InvalidEntityException();
        }
    }

    private void validateUserSettings(UserSettings userSettings) {
        if (userSettings == null || userSettings.getFontSize() <= 0 || userSettings.getFontSize() > 100) {
            log.error("User settings failed validation: " + userSettings);
            throw new InvalidEntityException();
        }
    }
}
