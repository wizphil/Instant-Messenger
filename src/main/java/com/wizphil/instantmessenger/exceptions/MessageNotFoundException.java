package com.wizphil.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class MessageNotFoundException extends ResponseStatusException {
    public MessageNotFoundException(String id) {
        super(HttpStatus.NOT_FOUND, "Message with id: " + id + " not found.");
    }
}
