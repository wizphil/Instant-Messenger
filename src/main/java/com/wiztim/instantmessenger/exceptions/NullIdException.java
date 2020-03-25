package com.wiztim.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class NullIdException extends ResponseStatusException {
    public NullIdException() {
        super(HttpStatus.BAD_REQUEST, "Id cannot be null.");
    }
}
