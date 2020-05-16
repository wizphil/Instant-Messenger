package com.wiztim.instantmessenger.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.wiztim.instantmessenger.dto.MessageWrapperDTO;
import com.wiztim.instantmessenger.enums.MessageCategory;
import com.wiztim.instantmessenger.enums.Status;
import com.wiztim.instantmessenger.exceptions.DuplicateSessionException;
import com.wiztim.instantmessenger.persistence.user.UserSession;
import com.wiztim.instantmessenger.persistence.user.UserStatus;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.EncodeException;
import javax.websocket.Session;
import java.io.IOException;
import java.util.Collection;
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
        if (sessionIdToUserId.getUnchecked(sessionId) != null) {
            log.warn("addSession: duplicate session for for user {}; session={}", userId, userSession);
            throw new DuplicateSessionException(sessionId);
        }

        sessionIdToUserId.put(userSession.getSession().getId(), userId);
        sessionIdToUserSession.put(sessionId, userSession);
        userIdToSessionIds.put(userId, sessionId);
    }

    public void closeUserSessions(UUID userId) {
        Set<String> sessionIds = userIdToSessionIds.get(userId);
        if (sessionIds.size() == 0) {
            return;
        }

        log.info("closing all sessions for user {} sessions {}", userId, sessionIds);
        for (String sessionId : sessionIds) {
            closeSession(sessionId);
        }

        // closeSession should have already taken care of this for us... not sure we need this
        userIdToSessionIds.removeAll(userId);
    }

    public UUID closeSession(String sessionId) {
        UUID userId = sessionIdToUserId.getUnchecked(sessionId);
        UserSession userSession = sessionIdToUserSession.getUnchecked(sessionId);

        sessionIdToUserId.invalidate(sessionId);
        sessionIdToUserSession.invalidate(sessionId);

        if (userId != null) {
            userIdToSessionIds.remove(userId, sessionId);
        }

        if (userSession != null) {
            // TODO send a reason for closing the userSession, so the client knows why userSession ended
            Session session = userSession.getSession();
            sendMessage(session, new MessageWrapperDTO(MessageCategory.CloseSession, ""));
        }

        return userId;
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
        if (userId == null) {
            return null;
        }

        Set<String> sessionIds = userIdToSessionIds.get(userId);
        if (sessionIds.size() == 0) {
            return null;
        }

        UserStatus userStatus = null;
        long mostRecentStatusTime = Long.MIN_VALUE;
        for (String sessionId : sessionIds) {
            UserSession userSession = sessionIdToUserSession.getUnchecked(sessionId);
            // We should only return ComputerLocked if it is the only status type
            // ComputerLocked is automatically set when the user's computer is locked
            // When there is a session with any other status, it means that session is an active user session
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

    public void sendMessageToUsers(Set<UUID> userIds, MessageWrapperDTO messageWrapperDTO) {
        for (UUID userId: userIds) {
            sendMessageToUser(userId, messageWrapperDTO);
        }
    }

    public void sendMessageToUser(UUID userId, MessageWrapperDTO messageWrapperDTO) {
        Set<String> sessionIds = userIdToSessionIds.get(userId);
        for (String sessionId : sessionIds) {
            UserSession userSession = sessionIdToUserSession.getUnchecked(sessionId);
            if (userSession != null) {
                sendMessage(userSession.getSession(), messageWrapperDTO);
            } else {
                log.error("userIdToSessionIds contained an expired/invalid sessionId. userId {} sessionId {}", userId, sessionId);
                userIdToSessionIds.remove(userId, sessionId);
            }
        }
    }

    public void sendMessageToAll(MessageWrapperDTO messageWrapperDTO) {
        Collection<Collection<String>> allSessions = userIdToSessionIds.asMap().values();
        for (Collection<String> sessionIdCollection : allSessions) {
            for (String sessionId : sessionIdCollection) {
                UserSession userSession = sessionIdToUserSession.getUnchecked(sessionId);
                if (userSession == null) {
                    log.error("sendMessageToAll found cached sessionId without an associated userSession. sessionId: {}", sessionId);
                    continue;
                }

                sendMessage(userSession.getSession(), messageWrapperDTO);
            }
        }
    }

    public void sendMessageToAllExceptSelf(UUID userId, MessageWrapperDTO messageWrapperDTO) {
        Set<String> mySessionIds = userIdToSessionIds.get(userId);

        Collection<Collection<String>> allSessions = userIdToSessionIds.asMap().values();
        for (Collection<String> sessionIdCollection : allSessions) {
            for (String sessionId : sessionIdCollection) {
                if (mySessionIds.contains(sessionId)) {
                    continue;
                }

                UserSession userSession = sessionIdToUserSession.getUnchecked(sessionId);
                if (userSession == null) {
                    log.error("sendMessageToAllExceptSelf found cached sessionId without an associated userSession. sessionId: {}", sessionId);
                    continue;
                }

                sendMessage(userSession.getSession(), messageWrapperDTO);
            }
        }
    }

    private void sendMessage(Session session, MessageWrapperDTO messageWrapperDTO) {
        if (session == null || messageWrapperDTO == null) {
            log.warn("Attempted to send message, but session and/or message was null. session {} message {}", session, messageWrapperDTO);
            return;
        }

        try {
            session.getBasicRemote().sendObject(messageWrapperDTO);
        } catch (EncodeException | IOException e) {
            log.error("Failed to sendMessage to session: {} message: {}", session, messageWrapperDTO, e);
            closeSession(session.getId());
        }
    }
}
