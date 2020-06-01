package com.wizphil.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UserNotFoundException extends ResponseStatusException {
    public UserNotFoundException(String id) {
        super(HttpStatus.NOT_FOUND, "User with id: " + id + " not found.");
    }
}
