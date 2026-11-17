package com.aistudybuddy.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested doubt record is not found or does not belong to the user.
 * Maps to HTTP 404 Not Found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class DoubtNotFoundException extends RuntimeException {

    public DoubtNotFoundException(String message) {
        super(message);
    }

    public DoubtNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
