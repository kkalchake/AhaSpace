package com.kkalchake.enlightenment.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 1. Catch Validation Errors (e.g., password too short)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        // Logged before return, not replacing it: the response body/status below is unchanged.
        log.error("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    // 2. Catch Business Logic Conflicts (e.g., "Username already exists")
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    // 3. Catch ResponseStatusException — returning ResponseEntity directly prevents sendError(),
    //    which would re-enter the Spring Security filter chain and return 403 instead of the real status
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        // Covers cases like a failed login returning a non-2xx status.
        log.error("Response status exception: {} - {}", ex.getStatusCode().value(), message);
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", message));
    }
}