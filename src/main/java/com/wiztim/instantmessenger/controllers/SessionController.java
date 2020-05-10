package com.wiztim.instantmessenger.controllers;

import com.wiztim.instantmessenger.dto.UserProfileDTO;
import com.wiztim.instantmessenger.enums.Status;
import com.wiztim.instantmessenger.interfaces.ISessionController;
import com.wiztim.instantmessenger.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/session")
@Slf4j
public class SessionController implements ISessionController {
    @Autowired
    private SessionService sessionService;

    @Override
    @PostMapping("/username/{username}/connection/{connection}")
    public UserProfileDTO login(@PathVariable("username") String username, @PathVariable("connection") String connection) {
        return sessionService.login(username, connection);
    }

    @Override
    @PutMapping("/id/{id}/connection/{connection}")
    public boolean reconnect(@PathVariable("id") UUID id, @PathVariable("connection") String connection, @PathVariable("status") Status status) {
        return sessionService.manualReconnect(id, connection, status);
    }

    @Override
    @DeleteMapping("/id/{id}/connection/{connection}")
    public void logout(@PathVariable("id") UUID id, @PathVariable("connection") String connection) {
        sessionService.logout(id, connection);
    }
}
