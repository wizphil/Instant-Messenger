package com.wiztim.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public class UserNotFoundException extends ResponseStatusException {
    public UserNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "User with id: " + id + " not found.");
    }
}
