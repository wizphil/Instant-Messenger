package com.wiztim.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public class MessageNotFoundException extends ResponseStatusException {
    public MessageNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Message with id: " + id + " not found.");
    }
}
