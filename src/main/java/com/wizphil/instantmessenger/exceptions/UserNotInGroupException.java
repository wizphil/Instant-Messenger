package com.wizphil.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UserNotInGroupException extends ResponseStatusException {
    public UserNotInGroupException(String groupId, String userId) {
        super(HttpStatus.NOT_FOUND, "User with id: " + userId + " not found in group: " + groupId + ".");
    }
}
