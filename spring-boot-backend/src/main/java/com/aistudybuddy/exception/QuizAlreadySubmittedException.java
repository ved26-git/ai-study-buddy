package com.aistudybuddy.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a student attempts to submit a quiz that has already been graded.
 * Maps to HTTP 409 Conflict.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class QuizAlreadySubmittedException extends RuntimeException {

    public QuizAlreadySubmittedException(String message) {
        super(message);
    }

    public QuizAlreadySubmittedException(String message, Throwable cause) {
        super(message, cause);
    }
}
