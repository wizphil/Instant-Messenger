package com.wiztim.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public class DisabledEntityException extends ResponseStatusException {
    public DisabledEntityException(UUID id) {
        super(HttpStatus.BAD_REQUEST, "Entity with id: " + id + " is disabled.");
    }
}
