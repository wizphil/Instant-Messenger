package com.wiztim.instantmessenger.controllers;

import com.wiztim.instantmessenger.enums.Status;
import com.wiztim.instantmessenger.persistence.Message;
import com.wiztim.instantmessenger.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.UUID;

// All users will create a WebSocket connection to mark themselves as online
// If the connection is closed or errors out, we use UserService to mark the session as closed
@ServerEndpoint(value = "/session/{userId}/status/{status}")
@Slf4j
public class WebSocketController {

    @Autowired
    private UserService userService;

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") UUID userId, @PathParam("status") Status status) {
        // Get session and WebSocket connection
        log.info("Session opened, sessionId: {}, userId: {}, status: {}", session.getId(), userId, status);
        userService.newUserSession(userId, session, status);
    }

    @OnMessage
    public void onMessage(Session session, Message message) {
        // Handle new messages
        log.info("Message received, sessionId: {} message: {}", session.getId(), message);
    }

    @OnClose
    public void onClose(Session session) {
        // WebSocket connection closes
        log.info("Session closed, sessionId: " + session.getId());
        userService.endUserSession(session);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        // Do error handling here
        log.info("Session error, sessionId: " + session.getId() + ", throwable: " + throwable);
        userService.endUserSession(session);
    }
}
