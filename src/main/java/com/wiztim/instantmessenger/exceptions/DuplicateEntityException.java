package com.wiztim.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public class DuplicateEntityException extends ResponseStatusException {
    public DuplicateEntityException(String username) {
        super(HttpStatus.BAD_REQUEST, "User with username: " + username + " already exists.");
    }
    public DuplicateEntityException(UUID id) {
        super(HttpStatus.BAD_REQUEST, "Entity with id: " + id + " already exists.");
    }
}
