package com.wizphil.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class NameTooLargeException extends ResponseStatusException {
    public NameTooLargeException(String name, int nameSize, int maxNameSize) {
        super(HttpStatus.BAD_REQUEST, "Name: " + name + " too large. Size: " + nameSize + ", Max Size: " + maxNameSize);
    }
}
