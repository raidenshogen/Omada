package com.omada.realtime.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Global REST error handling: every error returns a consistent JSON body
 * {"message": "..."} with the right status code, never a stack trace.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static ResponseEntity<Map<String, String>> of(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    /** Bad requests: invalid credentials, duplicate email, bad UUIDs, version conflicts. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex) {
        return of(HttpStatus.BAD_REQUEST, ex.getMessage() != null ? ex.getMessage() : "Bad request");
    }

    /** Optimistic-lock version conflicts from card updates. */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException ex) {
        return of(HttpStatus.CONFLICT, ex.getMessage() != null ? ex.getMessage() : "Conflict");
    }

    /** Missing resources: board/card/list/workspace not found. */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(NoSuchElementException ex) {
        return of(HttpStatus.NOT_FOUND, ex.getMessage() != null ? ex.getMessage() : "Resource not found");
    }

    /** Bean Validation failures on @Valid request bodies. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return of(HttpStatus.BAD_REQUEST, detail);
    }

    /** Spring Security denials from @PreAuthorize — 403, not a 500. */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> denied(org.springframework.security.access.AccessDeniedException ex) {
        return of(HttpStatus.FORBIDDEN, "Forbidden");
    }

    /** Unknown paths (no controller mapping) — a 404, not a 500. */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> noResource(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return of(HttpStatus.NOT_FOUND, "Not found");
    }

    /** Fallback: never leak stack traces to clients. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> internal(Exception ex) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }
}