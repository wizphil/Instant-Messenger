package com.wiztim.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RepositoryException  extends ResponseStatusException {
    public RepositoryException(Throwable e) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "Could not write to the repository.", e);
    }
}
