package com.aistudybuddy.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom Exception thrown when Gemini's JSON output for quizzes fails validation:
 * - Every question must contain an array of exactly 4 options
 * - The 'correctAnswer' value must exist within those 4 options
 * - Rejects malformed JSON, empty options, and duplicate choices
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidQuizException extends RuntimeException {

    public InvalidQuizException(String message) {
        super(message);
    }

    public InvalidQuizException(String message, Throwable cause) {
        super(message, cause);
    }
}
