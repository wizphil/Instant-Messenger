package com.wizphil.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class DuplicateEntityException extends ResponseStatusException {
    public DuplicateEntityException(String id) {
        super(HttpStatus.BAD_REQUEST, "Entity with id: " + id + " already exists.");
    }
}
