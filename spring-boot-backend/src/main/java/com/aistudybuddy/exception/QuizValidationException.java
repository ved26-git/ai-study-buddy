package com.aistudybuddy.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom Exception alias extending InvalidQuizException.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class QuizValidationException extends InvalidQuizException {

    public QuizValidationException(String message) {
        super(message);
    }

    public QuizValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
