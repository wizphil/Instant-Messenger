package com.wiztim.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class DuplicateSessionException extends ResponseStatusException {
    public DuplicateSessionException(String id) {
        super(HttpStatus.BAD_REQUEST, "Session with id: " + id + " already exists.");
    }
}
