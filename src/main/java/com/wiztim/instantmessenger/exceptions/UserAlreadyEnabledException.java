package com.wiztim.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public class UserAlreadyEnabledException extends ResponseStatusException {
    public UserAlreadyEnabledException(UUID id) {
        super(HttpStatus.BAD_REQUEST, "User: " + id + " is already enabled.");
    }
}
