package com.wizphil.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class DuplicateUsernameException extends ResponseStatusException {
    public DuplicateUsernameException(String username) {
        super(HttpStatus.BAD_REQUEST, "User with username: " + username + " already exists.");
    }
}
