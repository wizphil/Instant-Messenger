package com.wizphil.instantmessenger.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.wizphil.instantmessenger.dto.MessageWrapperDTO;
import com.wizphil.instantmessenger.enums.MessageCategory;
import com.wizphil.instantmessenger.enums.Status;
import com.wizphil.instantmessenger.exceptions.DuplicateSessionException;
import com.wizphil.instantmessenger.persistence.user.UserSession;
import com.wizphil.instantmessenger.persistence.user.UserStatus;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.EncodeException;
import javax.websocket.Session;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

@Component
@Setter
@Slf4j
public class SessionService {

    // This allows us to handle multiple user sessions (aka one user is signed in to multiple locations)
    // We can send a message to all sessions and all sessions for a user, and can keep track of the user status for each session
    private final SetMultimap<String, String> userIdToSessionIds = Multimaps.synchronizedSetMultimap(HashMultimap.create());

    // This cache is just for getting the userId so we can remove the session from `userIdToSessions` when the session closes
    private final LoadingCache<String, String> sessionIdToUserId = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public String load(String sessionId) {
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

    public void clearAllCache() {

        Collection<Collection<String>> allSessions = userIdToSessionIds.asMap().values();
        for (Collection<String> sessionIdCollection : allSessions) {
            for (String sessionId : sessionIdCollection) {
                closeSession(sessionId, true);
            }
        }

        userIdToSessionIds.clear();
        sessionIdToUserId.invalidateAll();
        sessionIdToUserSession.invalidateAll();
    }

    public void addSession(String userId, UserSession userSession) {
        String sessionId = userSession.getSession().getId();
        try {
            sessionIdToUserId.getUnchecked(sessionId);
            log.warn("addSession: duplicate session for for user {}; session={}", userId, userSession);
            throw new DuplicateSessionException(sessionId);
        } catch (CacheLoader.InvalidCacheLoadException ignored) {
        }

        sessionIdToUserId.put(userSession.getSession().getId(), userId);
        sessionIdToUserSession.put(sessionId, userSession);
        userIdToSessionIds.put(userId, sessionId);

        sendMessage(userSession.getSession(), new MessageWrapperDTO(MessageCategory.EstablisedSession, sessionId));
    }

    public void closeUserSessions(String userId) {
        Set<String> sessionIds = userIdToSessionIds.get(userId);
        if (sessionIds.size() == 0) {
            return;
        }

        log.info("closing all sessions for user {} sessions {}", userId, sessionIds);
        for (String sessionId : sessionIds) {
            closeSession(sessionId, true);
        }

        // closeSession should have already taken care of this for us... not sure we need this
        userIdToSessionIds.removeAll(userId);
    }

    public String closeSession(String sessionId, boolean manuallyClosed) {
        String userId = null;
        UserSession userSession;

        try {
            userId = sessionIdToUserId.getUnchecked(sessionId);
            userIdToSessionIds.remove(userId, sessionId);
        } catch (CacheLoader.InvalidCacheLoadException ignored) {
        } finally {
            sessionIdToUserId.invalidate(sessionId);
        }

        if (manuallyClosed) {
            try {
                userSession = sessionIdToUserSession.getUnchecked(sessionId);

                // TODO send a reason for closing the userSession, so the client knows why userSession ended
                Session session = userSession.getSession();
                sendMessage(session, new MessageWrapperDTO(MessageCategory.CloseSession, ""));
            } catch (CacheLoader.InvalidCacheLoadException ignored) {
            }
        }

        sessionIdToUserSession.invalidate(sessionId);
        return userId;
    }

    public boolean updateSessionStatus(String sessionId, UserStatus userStatus) {
        UserSession userSession;
        try {
            userSession = sessionIdToUserSession.getUnchecked(sessionId);
        } catch (CacheLoader.InvalidCacheLoadException e) {
            return false;
        }

        userSession.setUserStatus(userStatus);
        return true;
    }

    // When there are multiple sessions for a user, we want their displayed status to be the most recent status
    public UserStatus getMostRecentUserStatus(String userId) {
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
            UserSession userSession;
            try {
                userSession = sessionIdToUserSession.getUnchecked(sessionId);
            } catch (CacheLoader.InvalidCacheLoadException e) {
                log.error("getMostRecentUserStatus userIdToSessionIds contained a sessionId that wasn't found in sessionIdToUserSession userId {} sessionId {}", userId, sessionId);
                userIdToSessionIds.remove(userId, sessionId);
                continue;
            }

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

    public void sendMessageToUsers(Set<String> userIds, MessageWrapperDTO messageWrapperDTO) {
        for (String userId: userIds) {
            sendMessageToUser(userId, messageWrapperDTO);
        }
    }

    public void sendMessageToUser(String userId, MessageWrapperDTO messageWrapperDTO) {
        Set<String> sessionIds = userIdToSessionIds.get(userId);
        for (String sessionId : sessionIds) {
            UserSession userSession;
            try {
                userSession = sessionIdToUserSession.getUnchecked(sessionId);
                sendMessage(userSession.getSession(), messageWrapperDTO);
            } catch (CacheLoader.InvalidCacheLoadException e) {
                log.error("getMostRecentUserStatus userIdToSessionIds contained a sessionId that wasn't found in sessionIdToUserSession userId {} sessionId {}", userId, sessionId);
                userIdToSessionIds.remove(userId, sessionId);
            }
        }
    }

    public void sendMessageToAll(MessageWrapperDTO messageWrapperDTO) {
        Collection<Collection<String>> allSessions = userIdToSessionIds.asMap().values();
        for (Collection<String> sessionIdCollection : allSessions) {
            for (String sessionId : sessionIdCollection) {
                UserSession userSession;
                try {
                    userSession = sessionIdToUserSession.getUnchecked(sessionId);
                    sendMessage(userSession.getSession(), messageWrapperDTO);
                } catch (CacheLoader.InvalidCacheLoadException e) {
                    log.error("sendMessageToAll found cached sessionId without an associated userSession. sessionId: {}", sessionId);
                }
            }
        }
    }

    public void sendMessageToAllExceptSelf(String userId, MessageWrapperDTO messageWrapperDTO) {
        Set<String> mySessionIds = userIdToSessionIds.get(userId);

        Collection<Collection<String>> allSessions = userIdToSessionIds.asMap().values();
        for (Collection<String> sessionIdCollection : allSessions) {
            for (String sessionId : sessionIdCollection) {
                if (mySessionIds.contains(sessionId)) {
                    continue;
                }

                UserSession userSession;
                try {
                    userSession = sessionIdToUserSession.getUnchecked(sessionId);
                    sendMessage(userSession.getSession(), messageWrapperDTO);
                } catch (CacheLoader.InvalidCacheLoadException e) {
                    log.error("sendMessageToAllExceptSelf found cached sessionId without an associated userSession. sessionId: {}", sessionId);
                }
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
            // TODO investiage. I don't think we should close the session every time we fail to message, we already have a listeneer for closed/error sessions
            //closeSession(session.getId(), false);
        }
    }
}
