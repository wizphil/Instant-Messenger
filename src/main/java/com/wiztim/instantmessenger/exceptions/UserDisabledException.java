package com.wiztim.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public class UserDisabledException extends ResponseStatusException {
    public UserDisabledException(UUID id) {
        super(HttpStatus.BAD_REQUEST, "User: " + id + " is disabled.");
    }

    public UserDisabledException(String username) {
        super(HttpStatus.BAD_REQUEST, "User: " + username + " is disabled.");
    }
}
