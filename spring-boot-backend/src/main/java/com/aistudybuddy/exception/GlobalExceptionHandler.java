package com.aistudybuddy.exception;

import io.jsonwebtoken.JwtException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ==========================================
    // 400 BAD REQUEST
    // ==========================================
    @ExceptionHandler({InvalidQuizException.class, QuizValidationException.class})
    public ResponseEntity<Map<String, Object>> handleInvalidQuizException(InvalidQuizException ex) {
        log.warn("Invalid quiz request: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((err) -> {
            String fieldName = err instanceof FieldError ? ((FieldError) err).getField() : err.getObjectName();
            String errorMessage = err.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Input validation failed");
        response.put("validationErrors", errors);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // ==========================================
    // 401 UNAUTHORIZED
    // ==========================================
    @ExceptionHandler({
    BadCredentialsException.class,
    AuthenticationException.class,
    JwtException.class
    })
     public ResponseEntity<Map<String, Object>> handleAuthenticationException(Exception ex) {
    log.warn("Authentication failed: {}", ex.getMessage());

    return buildErrorResponse(
        HttpStatus.UNAUTHORIZED,
        "Invalid or missing credentials. Please sign in again."
      );
    }

    // ==========================================
    // 403 FORBIDDEN
    // ==========================================
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Access denied. You do not have permission to access this resource.");
    }

    // ==========================================
    // 404 NOT FOUND
    // ==========================================
    @ExceptionHandler(QuizNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleQuizNotFound(QuizNotFoundException ex) {
        log.warn("Quiz not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(NoteNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoteNotFound(NoteNotFoundException ex) {
        log.warn("Note not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DoubtNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleDoubtNotFound(DoubtNotFoundException ex) {
        log.warn("Doubt not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UsernameNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "The requested user account was not found.");
    }

    // ==========================================
    // 409 CONFLICT (ALREADY SUBMITTED QUIZZES & CONCURRENCY)
    // ==========================================
    @ExceptionHandler({QuizAlreadySubmittedException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<Map<String, Object>> handleQuizConflict(Exception ex) {
        log.warn("Quiz conflict or duplicate submission attempt: {}", ex.getMessage());
        String msg = ex instanceof QuizAlreadySubmittedException
                ? ex.getMessage()
                : "The quiz has already been modified or submitted by another concurrent request.";
        return buildErrorResponse(HttpStatus.CONFLICT, msg);
    }

    // ==========================================
    // 500 INTERNAL SERVER ERROR
    // ==========================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("Unhandled internal server exception: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred. Please try again later.");
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", status.value());
        error.put("error", message);
        return new ResponseEntity<>(error, status);
    }
}
