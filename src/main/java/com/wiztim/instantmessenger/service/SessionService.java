package com.wiztim.instantmessenger.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.wiztim.instantmessenger.dto.UserDTO;
import com.wiztim.instantmessenger.dto.UserProfileDTO;
import com.wiztim.instantmessenger.enums.Status;
import com.wiztim.instantmessenger.exceptions.InvalidEntityException;
import com.wiztim.instantmessenger.exceptions.UserDisabledException;
import com.wiztim.instantmessenger.persistence.user.User;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Setter
@Component
public class SessionService {

    @Autowired
    UserService userService;

    private final Map<UUID, Set<String>> sessionCache = new HashMap<>();
    private final LoadingCache<String, Status> recentlyExpiredSessions = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Status>() {
                @Override
                public Status load(String connection) throws Exception {
                    return null;
                }
            });

    public UserProfileDTO login(String username, String connection) {
        if (username == null || username.isBlank() || connection == null || connection.isBlank()) {
            log.error("Bad request made to login. username=" + username + "; connection=" + connection);
            throw new InvalidEntityException();
        }

        User user = userService.getUserByUsername(username);

        // null user means user does not exist, or user is disabled
        if (user == null) {
            // disabled users are not allowed to login
            if (userService.isUserDisabled(username)) {
                log.info("Disabled user attempted to login. username=" + username);
                throw new UserDisabledException(username);
            }

            log.info("New user is logging in, creating new user for them. username=" + username);

            // all users should be part of the team, let's create a new user when it doesn't exist
            // maybe this should be a separate step from login, but for now I like the idea of auto-creation
            user = userService.createUser(username);
        }

        // add this session to the cache
        UUID id = user.getId();
        if (sessionCache.containsKey(id)) {
            sessionCache.get(id).add(connection);
        }
        else {
            // we only add the user to the session cache
            // dont use user service to set their status to online yet, the client will do that once it's initialized
            sessionCache.put(id, new HashSet<>(List.of(connection)));
        }

        return UserProfileDTO.builder()
                .id(user.getId())
                .userInfo(user.getUserInfo())
                .userSettings(user.getUserSettings())
                .enabled(user.isEnabled())
                .build();
    }

    public void logout(UUID id, String connection) {
        if (id == null || connection == null || connection.isBlank()) {
            log.error("Bad request made to logout. id=" + id + "; connection=" + connection);
            throw new InvalidEntityException();
        }

        Set<String> sessions = sessionCache.get(id);
        if (sessions == null || !sessions.contains(connection)) {
            log.warn("Received logout request for a user that isn't logged in. id=" + id + "; connection=" + connection);
            return;
        }

        sessions.remove(connection);

        // save the user status in case this session reconnects
        // by saving the current user status, we can better handle the case where:
        // session 1 disconnects > session 2 disconnects > session 1 reconnects
        // it's not perfect, but it should work fine
        // perfect would be if we tied each status to a session
        // example: session 1 = locked computer, user is away; session 2 = user is online.
        UserDTO userDTO = userService.getUserDTO(id);
        if (userDTO != null) {
            recentlyExpiredSessions.put(connection, userDTO.getUserStatus().getStatus());
        }

        // if no session remain, mark offline
        if (sessions.size() == 0) {
            log.info("User " + id + " has no active sessions remaining. Logging out.");
            sessionCache.remove(id);
            userService.updateUserStatus(id, Status.Offline);
        }
    }

    public boolean sessionExists(UUID id) {
        return sessionCache.containsKey(id);
    }

    // this is done by the service listening to the event exchange
    public boolean autoReconnect(UUID id, String connection) {
        if (id == null || connection == null || connection.isBlank()) {
            log.error("Bad request made to autoReconnect. id=" + id + "; connection=" + connection);
            throw new InvalidEntityException();
        }

        // no need to do anything if the user is already online
        if (!userService.isUserOffline(id)) {
            return false;
        }

        Status status;
        try {
            status = recentlyExpiredSessions.get(connection);
        } catch (ExecutionException e) {
            log.warn("Failed to get recently expired session from loading cache.", e);
            return false;
        }

        if (status == null) {
            log.warn("User attempted to reconnect, but their session is no longer in the cache");
            return false;
        }

        // add this session to the cache
        if (sessionCache.containsKey(id)) {
            sessionCache.get(id).add(connection);
        }
        else {
            sessionCache.put(id, new HashSet<>(List.of(connection)));
        }

        // mark the user online again, using the status from the session that expired
        userService.updateUserStatus(id, status);
        return true;
    }

    // this is a REST request made by the client when it reconnects
    // a little redundant with above, but autoReconnect is in case the client doesn't notice the connection drop (which means they wouldn't call this method)
    public boolean manualReconnect(UUID id, String connection, Status status) {
        if (id == null || connection == null || connection.isBlank() || status == null) {
            log.error("Bad request made to manualReconnect. id=" + id + "; connection=" + connection + "; status=" + status);
            throw new InvalidEntityException();
        }

        // no need to do anything if the user is already online
        if (!userService.isUserOffline(id)) {
            return false;
        }

        // add this session to the cache
        if (sessionCache.containsKey(id)) {
            sessionCache.get(id).add(connection);
        }
        else {
            sessionCache.put(id, new HashSet<>(List.of(connection)));
        }

        // mark the user online again, using the status from the session that expired
        userService.updateUserStatus(id, status);
        return true;
    }
}
