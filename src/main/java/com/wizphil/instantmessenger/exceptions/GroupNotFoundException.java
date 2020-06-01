package com.wizphil.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class GroupNotFoundException extends ResponseStatusException {
    public GroupNotFoundException(String id) {
        super(HttpStatus.NOT_FOUND, "Group with id: " + id + " not found.");
    }
}
