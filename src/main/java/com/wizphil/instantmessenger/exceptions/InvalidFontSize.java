package com.wizphil.instantmessenger.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InvalidFontSize extends ResponseStatusException {
    public InvalidFontSize(int fontSize, int maxFontSize) {
        super(HttpStatus.BAD_REQUEST, "Font size invalid. Font size:" + fontSize + ", Must be between 0 and " + maxFontSize);
    }
}
