package com.wiztim.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InvalidEntityException extends ResponseStatusException {
    public InvalidEntityException() {
        super(HttpStatus.BAD_REQUEST, "Entity must be non-null and have all required fields.");
    }
}
