package com.wizphil.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UserAlreadyEnabledException extends ResponseStatusException {
    public UserAlreadyEnabledException(String id) {
        super(HttpStatus.BAD_REQUEST, "User: " + id + " is already enabled.");
    }
}
