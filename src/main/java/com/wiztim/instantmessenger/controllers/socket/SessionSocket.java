package com.wiztim.instantmessenger.controllers.socket;

import com.wiztim.instantmessenger.persistence.Message;
import lombok.extern.slf4j.Slf4j;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

@ServerEndpoint(value = "/session/{userId}")
@Slf4j
public class SessionSocket {

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) throws IOException {
        // Get session and WebSocket connection
        String sessionId = session.getId();
        log.info("Session opened, sessionId: {}, userId: {}", sessionId, userId);
    }

    @OnMessage
    public void onMessage(Session session, Message message) throws IOException {
        // Handle new messages
        String sessionId = session.getId();
        log.info("Message received, sessionId: {} message: {}", sessionId, message);
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        // WebSocket connection closes
        String sessionId = session.getId();
        log.info("Session closed, sessionId: " + sessionId);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        // Do error handling here
        String sessionId = session.getId();
        log.info("Session error, sessionId: " + sessionId + ", throwable: " + throwable);
    }
}
