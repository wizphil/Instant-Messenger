package com.wiztim.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public class GroupNotFoundException extends ResponseStatusException {
    public GroupNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Group with id: " + id + " not found.");
    }
}
