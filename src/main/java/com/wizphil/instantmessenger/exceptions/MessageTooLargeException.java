package com.wizphil.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class MessageTooLargeException extends ResponseStatusException {
    public MessageTooLargeException(int messageSize, int maxMessageSize) {
        super(HttpStatus.BAD_REQUEST, "Message content too large. Size: " + messageSize + ", Max Size: " + maxMessageSize);
    }
}
