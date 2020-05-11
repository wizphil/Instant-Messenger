package com.wiztim.instantmessenger.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.wiztim.instantmessenger.dto.MessageWrapperDTO;
import com.wiztim.instantmessenger.enums.Status;
import com.wiztim.instantmessenger.persistence.user.UserSession;
import com.wiztim.instantmessenger.persistence.user.UserStatus;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.EncodeException;
import javax.websocket.Session;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@Setter
@Slf4j
public class SessionService {

    // This allows us to handle multiple user sessions (aka one user is signed in to multiple locations)
    // We can send a message to all sessions and all sessions for a user, and can keep track of the user status for each session
    private final SetMultimap<UUID, String> userIdToSessionIds = Multimaps.synchronizedSetMultimap(HashMultimap.create());

    // This cache is just for getting the userId so we can remove the session from `userIdToSessions` when the session closes
    private final LoadingCache<String, UUID> sessionIdToUserId = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public UUID load(String sessionId) {
            return null;
        }
    });

    // The actual user session that has the user's status and socket session where we can send messages
    private final LoadingCache<String, UserSession> sessionIdToUserSession = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public UserSession load(String sessionId) {
            return null;
        }
    });

    public void addSession(UUID userId, UserSession userSession) {
        String sessionId = userSession.getSession().getId();

        sessionIdToUserId.put(userSession.getSession().getId(), userId);
        sessionIdToUserSession.put(sessionId, userSession);
        userIdToSessionIds.put(userId, sessionId);
    }

    public UUID removeSession(String sessionId) {
        UUID userId = sessionIdToUserId.getUnchecked(sessionId);
        if (userId != null) {
            userIdToSessionIds.remove(userId, sessionId);
        }

        sessionIdToUserId.invalidate(sessionId);
        sessionIdToUserSession.invalidate(sessionId);

        return userId;
    }

    public void removeUserSessions(UUID userId) {
        Set<String> sessionIds = userIdToSessionIds.get(userId);
        for (String sessionId : sessionIds) {
            sessionIdToUserId.invalidate(sessionId);
            sessionIdToUserSession.invalidate(sessionId);
        }

        userIdToSessionIds.removeAll(userId);
    }

    public int getSessionCount(UUID userId) {
        return userIdToSessionIds.get(userId).size();
    }

    public boolean updateSessionStatus(String sessionId, UserStatus userStatus) {
        UserSession userSession = sessionIdToUserSession.getUnchecked(sessionId);
        if (userSession == null) {
            return false;
        }

        userSession.setUserStatus(userStatus);
        return true;
    }

    // When there are multiple sessions for a user, we want their displayed status to be the most recent status
    public UserStatus getMostRecentUserStatus(UUID userId) {
        Set<String> sessionIds = userIdToSessionIds.get(userId);
        if (sessionIds.size() == 0) {
            return null;
        }

        UserStatus userStatus = null;
        long mostRecentStatusTime = Long.MIN_VALUE;
        for (String sessionId : sessionIds) {
            UserSession userSession = sessionIdToUserSession.getUnchecked(sessionId);
            // We should only return ComputerLocked if there are no other Status types
            // ComputerLocked is automatically set when the user's computer is locked, any other status means it's an active user session
            UserStatus sessionStatus = userSession.getUserStatus();
            long statusTime = (Status.ComputerLocked.equals(sessionStatus.getStatus())) ? -1 : sessionStatus.getTime();
            if (statusTime > mostRecentStatusTime) {
                userStatus = userSession.getUserStatus();
                mostRecentStatusTime = statusTime;
            }
        }

        // This should never happen, offline users should never be in the session cache
        if (userStatus == null || userStatus.getStatus() == Status.Offline) {
            for (String sessionId : sessionIds) {
                sessionIdToUserSession.invalidate(sessionId);
            }

            userIdToSessionIds.removeAll(userId);
            return null;
        }

        return userStatus;
    }

    public void sendMessageToUser(UUID userId, MessageWrapperDTO messageWrapperDTO) {
        sendMessageToUsers(List.of(userId), messageWrapperDTO);
    }

    public void sendMessageToUsers(List<UUID> userIds, MessageWrapperDTO messageWrapperDTO) {
        for (UUID userId: userIds) {
            Set<String> sessionIds = userIdToSessionIds.get(userId);
            for (String sessionId : sessionIds) {
                UserSession userSession = sessionIdToUserSession.getUnchecked(sessionId);
                sendMessage(userSession.getSession(), messageWrapperDTO);
            }
        }
    }

    public void sendMessageToAll(MessageWrapperDTO messageWrapperDTO) {
        Collection<Collection<String>> allSessions = userIdToSessionIds.asMap().values();
        for (Collection<String> sessionIdCollection : allSessions) {
            for (String sessionId : sessionIdCollection) {
                UserSession userSession = sessionIdToUserSession.getUnchecked(sessionId);
                sendMessage(userSession.getSession(), messageWrapperDTO);
            }
        }
    }

    private void sendMessage(Session session, MessageWrapperDTO messageWrapperDTO) {
        try {
            session.getBasicRemote().sendObject(messageWrapperDTO);
        } catch (EncodeException | IOException e) {
            log.error("Failed to send message {}", messageWrapperDTO, e);
        }
    }
}
