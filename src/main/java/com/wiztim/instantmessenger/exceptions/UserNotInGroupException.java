package com.wiztim.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public class UserNotInGroupException extends ResponseStatusException {
    public UserNotInGroupException(UUID groupId, UUID userId) {
        super(HttpStatus.NOT_FOUND, "User with id: " + userId + " not found in group: " + groupId + ".");
    }
}
