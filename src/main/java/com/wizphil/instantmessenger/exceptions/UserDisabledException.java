package com.wizphil.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UserDisabledException extends ResponseStatusException {
    public UserDisabledException(String id) {
        super(HttpStatus.BAD_REQUEST, "User: " + id + " is disabled.");
    }
}
