package com.wiztim.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public class SessionNotFoundException extends ResponseStatusException {
    public SessionNotFoundException(String id) {
        super(HttpStatus.NOT_FOUND, "Could not find session with id: " + id);
    }
}
