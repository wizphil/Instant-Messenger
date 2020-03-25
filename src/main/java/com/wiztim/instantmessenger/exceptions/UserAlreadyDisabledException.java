package com.wiztim.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public class UserAlreadyDisabledException extends ResponseStatusException {
    public UserAlreadyDisabledException(UUID id) {
        super(HttpStatus.BAD_REQUEST, "User: " + id + " is already disabled.");
    }
}
