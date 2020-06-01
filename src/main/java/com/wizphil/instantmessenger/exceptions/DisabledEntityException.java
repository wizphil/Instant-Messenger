package com.wizphil.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class DisabledEntityException extends ResponseStatusException {
    public DisabledEntityException(String id) {
        super(HttpStatus.BAD_REQUEST, "Entity with id: " + id + " is disabled.");
    }
}
