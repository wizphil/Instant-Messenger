package com.wizphil.instantmessenger.controllers;

import com.wizphil.instantmessenger.config.SpringContext;
import com.wizphil.instantmessenger.config.WebSocketEncoder;
import com.wizphil.instantmessenger.enums.Status;
import com.wizphil.instantmessenger.persistence.Message;
import com.wizphil.instantmessenger.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.server.standard.SpringConfigurator;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Stack;

// All users will create a WebSocket connection to mark themselves as online
// If the connection is closed or errors out, we use UserService to mark the session as closed
// @ServerEndpoint(value = "/session/{userId}/status/{status}", configurator = SpringConfigurator.class)
@ServerEndpoint(value = "/session/user/{userId}", encoders = WebSocketEncoder.class)
@Slf4j
public class WebSocketController {

    @Autowired
    private UserService userService;

    public WebSocketController() {
        this.userService = (UserService) SpringContext.getApplicationContext().getBean("userService");
    }

    //public void onOpen(Session session, @PathParam("userId") String userId, @PathParam("status") Status status) {
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        // Get session and WebSocket connection
        log.info("Session opened, sessionId: {}, userId: {}", session.getId(), userId);
        userService.newUserSession(userId, session);
    }

//    @OnMessage
//    public void onMessage(Session session, Message message) {
//        // Handle new messages
//        log.info("Message received, sessionId: {} message: {}", session.getId(), message);
//    }

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
