package com.wizphil.instantmessenger.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.wizphil.instantmessenger.dto.MessageWrapperDTO;
import com.wizphil.instantmessenger.dto.UserInfoDTO;
import com.wizphil.instantmessenger.dto.UserDetailsDTO;
import com.wizphil.instantmessenger.enums.MessageCategory;
import com.wizphil.instantmessenger.exceptions.InvalidFontSize;
import com.wizphil.instantmessenger.exceptions.NameTooLargeException;
import com.wizphil.instantmessenger.exceptions.NullIdException;
import com.wizphil.instantmessenger.exceptions.UserNotFoundException;
import com.wizphil.instantmessenger.persistence.user.UserDetails;
import com.wizphil.instantmessenger.dto.UserStatusDTO;
import com.wizphil.instantmessenger.enums.Status;
import com.wizphil.instantmessenger.exceptions.DuplicateEntityException;
import com.wizphil.instantmessenger.exceptions.DisabledEntityException;
import com.wizphil.instantmessenger.exceptions.InvalidEntityException;
import com.wizphil.instantmessenger.exceptions.UserAlreadyEnabledException;
import com.wizphil.instantmessenger.exceptions.UserDisabledException;
import com.wizphil.instantmessenger.persistence.user.User;
import com.wizphil.instantmessenger.persistence.user.UserSession;
import com.wizphil.instantmessenger.persistence.user.UserSettings;
import com.wizphil.instantmessenger.persistence.user.UserStatus;
import com.wizphil.instantmessenger.cache.UserCache;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.websocket.Session;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Setter
@Slf4j
public class UserService {

    @Autowired
    private UserCache userCache;

    @Autowired
    private SessionService sessionService;

    // TODO MAX_FONT_SIZE should be a dynamic server config
    private static final int MAX_FONT_SIZE = 200;
    // TODO MAX_NAME_SIZE should be a dynamic server config
    private static final int MAX_NAME_SIZE = 50;

    private final LoadingCache<String, UserInfoDTO> userInfoCache = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public UserInfoDTO load(String id) {
            User user = userCache.get(id);
            if (user == null) {
                log.warn("userInfoCache failed to load user with id: {}", id);
                return null;
            }

            if (!user.getUserDetails().isEnabled()) {
                log.info("userInfoCache attempted to load deactivated user with id: {}", id);
                return null;
            }

            return User.toUserInfoDTO(user, UserStatus.offlineNow());
        }
    });

    @PostConstruct
    public void init() {
        log.info("userService loading all users");
        int numUsers = loadAllOffline();
        log.info("userService loaded {} users", numUsers);
    }

    public int loadAllOffline() {
        List<User> allUsers = userCache.loadAll();
        for (User user : allUsers) {
            userInfoCache.put(user.getId(), User.toUserInfoDTO(user));
        }

        return allUsers.size();
    }

    public void newUserSession(String id, Session session) {
        validateUserEnabled(id);

        if (session == null) {
            log.warn("User {} attempted to create new session, but session was null", id);
            throw new InvalidEntityException();
        }

        validateSessionId(session.getId());

        UserStatus userStatus = UserStatus.builder()
                .status(Status.ConnectionInProgress)
                .time(-1)
                .build();

        UserSession userSession = UserSession.builder()
                .session(session)
                .userStatus(userStatus)
                .build();

        sessionService.addSession(id, userSession);
    }

    public void endUserSession(Session session) {
        if (session == null) {
            return;
        }

        String id = sessionService.closeSession(session.getId(), false);
        if (id == null) {
            log.info("Session with id {} already closed.", session.getId());
            return;
        }

        UserStatus latestSessionStatus = sessionService.getMostRecentUserStatus(id);
        if (latestSessionStatus == null) {
            setStatus(id, session.getId(), Status.Offline);
        } else {
            setStatus(id, session.getId(), latestSessionStatus.getStatus());
        }
    }

    public User createUser(String username) {
        UserDetails userDetails = UserDetails.builder()
                .username(username)
                .build();

        return createUser(userDetails);
    }

    public User createUser(UserDetails userDetails) {
        log.info("Creating new user {}", userDetails);
        validateUserDetails(userDetails);

        // check if user with username already exists
        String username = userDetails.getUsername();
        if (userCache.isExistingUsername(username)) {
            log.warn("Failed to create new user, user with username {} already exists", username);
            throw new DuplicateEntityException(username);
        }

        User user = User.builder()
                .userDetails(userDetails)
                .userSettings(UserSettings.defaultSettings())
                .build();

        user = userCache.createUser(user);

        if (user.getUserDetails().isEnabled()) {
            // update the cache and clients, we have a new user!
            userInfoCache.put(user.getId(), User.toUserInfoDTO(user));
            sessionService.sendMessageToAll(new MessageWrapperDTO(MessageCategory.NewUser, user));
        }

        log.info("New user created {}", user);
        return user;
    }

    public User getUserByUsername(String username) {
        validateUsername(username);
        username = username.trim();

        return userCache.getByUsername(username);
    }

    // When a user logs in, they need the entire list of enabled users with their status
    // We trust that the cache is accurate and up to date
    public Collection<UserInfoDTO> getAllUserInfo() {
        // TODO page results, don't want to send too many users in one call
        return userInfoCache.asMap().values();
    }

    public UserInfoDTO getUserInfoByUsername(String username) {
        validateUsername(username);
        username = username.trim();

        User user = userCache.getByUsername(username);
        if (user == null) {
            return null;
        }

        if (!user.getUserDetails().isEnabled()) {
            log.warn("getUserInfoByUsername attempted to get user info for disabled username: \"{}\"", username);
            throw new UserDisabledException(user.getId());
        }

        return getUserInfo(user.getId());
    }

    public User getUser(String id) {
        return userCache.get(id);
    }

    public UserInfoDTO getUserInfo(String id) {
        try {
            return userInfoCache.getUnchecked(id);
        } catch (CacheLoader.InvalidCacheLoadException e) {
            return null;
        }
    }

    public User updateUserProfile(User user) {
        validateUser(user);

        String id = user.getId();
        User oldUser = getExistingUser(id);
        UserSettings userSettings = user.getUserSettings();
        UserDetails userDetails = user.getUserDetails();

        // we don't allow changing enabled / disabled or associated usernames through this call
        userDetails.setEnabled(oldUser.getUserDetails().isEnabled());
        userDetails.setUsername(oldUser.getUserDetails().getUsername());

        boolean userDetailsChanged = !userDetails.equals(oldUser.getUserDetails());
        boolean userSettingsChanged = !userSettings.equals(oldUser.getUserSettings());

        if (!userDetailsChanged && !userSettingsChanged) {
            log.info("updateUserProfile was called with no changes made to the user");
            return user;
        }

        oldUser.setUserDetails(userDetails);
        oldUser.setUserSettings(userSettings);

        user = userCache.updateUser(oldUser);

        // let everyone know that a user details has changed
        if (userDetailsChanged) {
            log.info("User details have changed, broadcasting new details {}", userDetails);
            userInfoCache.getUnchecked(id).setUserDetails(userDetails);

            try {
                UserInfoDTO userInfo = userInfoCache.getUnchecked(id);
                userInfo.setUserDetails(userDetails);
                userInfoCache.put(id, userInfo);
            } catch (CacheLoader.InvalidCacheLoadException e) {
                log.error("updateUserProfile attemped to update userDetails. Could not find userInfo. This is very odd. user {}", oldUser);
            }

            UserDetailsDTO userDetailsDTO = new UserDetailsDTO(id, userDetails);
            sessionService.sendMessageToAll(new MessageWrapperDTO(MessageCategory.UpdateUserDetails, userDetailsDTO));
        }

        return user;
    }

    public void setStatus(String id, String sessionId, Status status) {
        validateId(id);

        UserStatus userStatus = UserStatus.builder()
                .status(status)
                .time(Instant.now().toEpochMilli())
                .build();

        setStatus(id, sessionId, userStatus);
    }

    private void setStatus(String id, String sessionId, UserStatus userStatus) {
        log.debug("setStatus called; id: {} sessionId: {} userStatus: {}", id, sessionId, userStatus);
        validateId(id);
        validateSessionId(sessionId);
        validateUserStatus(userStatus);

        Status status = userStatus.getStatus();

        if (Status.Offline.equals(status)) {
            log.info("setStatus received offline status, closing user session userId {} sessionId {}", id, sessionId);
            sessionService.closeSession(sessionId, true);
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
        UserInfoDTO userInfo;
        try {
            userInfo = userInfoCache.getUnchecked(id);
        } catch (CacheLoader.InvalidCacheLoadException e) {
            log.warn("setStatus failed to get userInfo for user {}", id);
            throw new UserNotFoundException(id);
        }

        UserStatus currentStatus = userInfo.getUserStatus();
        if (latestSessionStatus != null) {
            if (currentStatus.getStatus().equals(latestSessionStatus.getStatus())) {
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

    public User enableUser(String id) {
        log.info("enabling user {}", id);
        validateId(id);

        User user = getExistingUser(id);
        if (user.getUserDetails().isEnabled()) {
            log.warn("Enable user called on an already enabled user {}", id);
            throw new UserAlreadyEnabledException(id);
        }

        user.getUserDetails().setEnabled(true);
        user = userCache.updateUser(user);

        // add user to the cache
        UserInfoDTO userInfoDTO = User.toUserInfoDTO(user);
        userInfoCache.put(id, userInfoDTO);

        sessionService.sendMessageToAll(new MessageWrapperDTO(MessageCategory.NewUser, user));
        log.info("successfully enabled user {}", id);
        return user;
    }

    public User disableUser(String id) {
        log.info("disabling user {}", id);
        validateId(id);

        User user = getExistingUser(id);
        if (!user.getUserDetails().isEnabled()) {
            log.warn("Disable user called on an already Disabled user {}", id);
            throw new UserDisabledException(id);
        }

        user.getUserDetails().setEnabled(false);
        user = userCache.updateUser(user);

        // remove user from cache
        userInfoCache.invalidate(id);
        sessionService.closeUserSessions(id);

        sessionService.sendMessageToAll(new MessageWrapperDTO(MessageCategory.DisableUser, id));
        log.info("successfully disabled user {}", id);
        return user;
    }

    // takes a list of userIds and returns which of those users are currently enabled
    protected Set<String> getEnabledUserIds(Set<String> userIds) {
        Set<String> enabledUserIds = new HashSet<>();

        for (String id : userIds) {
            try {
                UserInfoDTO userInfo = userInfoCache.getUnchecked(id);
                if (userInfo.getUserDetails().isEnabled()) {
                    enabledUserIds.add(id);
                }
            } catch (CacheLoader.InvalidCacheLoadException ignored) {
            }
        }

        return enabledUserIds;
    }

    public User getExistingUser(String id) {
        validateId(id);

        User user = userCache.get(id);
        if (user == null) {
            log.warn("getExistingUser could not find user with id: {}", id);
            throw new UserNotFoundException(id);
        }

        return user;
    }

    public String getFullname(String id) {
        log.info("getting fullname for user {}", id);

        try {
            UserInfoDTO userInfo = userInfoCache.getUnchecked(id);
            return userInfo.getUserDetails().getFullname();
        } catch (CacheLoader.InvalidCacheLoadException e) {
            log.info("getting fullname for user {}, counldn't find user in userInfoCache, checking if deactivated user", id);
            // We don't keep deactivated users in the cache
            User user = getExistingUser(id);
            return user.getUserDetails().getFullname();
        }
    }

    public void clearUserCache() {
        userCache.clearAll();
    }

    public void clearUserInfoCache() {
        userInfoCache.invalidateAll();
    }

    public void clearSessionCache() {
        sessionService.clearAllCache();
    }

    public void clearAllCache() {
        clearUserCache();
        clearUserInfoCache();
        clearSessionCache();
    }

    protected void validateUserEnabled(String id) {
        validateId(id);

        User user = getExistingUser(id);
        if (!user.getUserDetails().isEnabled()) {
            log.warn("validateUserEnabled exception, user {} is disabled", id);
            throw new DisabledEntityException(id);
        }
    }

    public static void validateId(String id) {
        if (id == null ) {
            log.warn("User Id failed validation: null");
            throw new NullIdException();
        }
    }

    public static void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("User failed sessionId validation: {}", sessionId);
            throw new NullIdException();
        }
    }

    public static void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            log.warn("Username {} failed validation", username);
            throw new InvalidEntityException();
        }

        int usernameSize = username.trim().length();
        if (usernameSize > MAX_NAME_SIZE) {
            throw new NameTooLargeException(username.trim(), usernameSize, MAX_NAME_SIZE);
        }
    }

    public static void validateUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            log.warn("User details failed validation: null");
            throw new InvalidEntityException();
        }

        validateUsername(userDetails.getUsername());
    }

    public static void validateUserSettings(UserSettings userSettings) {
        if (userSettings == null) {
            log.warn("User settings failed validation: null");
            throw new InvalidEntityException();
        }

        int fontSize = userSettings.getFontSize();
        if (fontSize <= 0 || fontSize > MAX_FONT_SIZE) {
            throw new InvalidFontSize(fontSize, MAX_FONT_SIZE);
        }
    }

    public static void validateUserStatus(UserStatus userStatus) {
        if (userStatus == null || userStatus.getStatus() == null || userStatus.getTime() <= 0) {
            log.warn("User status failed validation: " + userStatus);
            throw new InvalidEntityException();
        }
    }

    public static void validateUser(User user) {
        if (user == null) {
            log.warn("User failed validation: null");
            throw new InvalidEntityException();
        }

        validateUserSettings(user.getUserSettings());
        validateUserDetails(user.getUserDetails());
    }
}
