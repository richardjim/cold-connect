package com.coldconnect.config;

import com.coldconnect.exception.AppException;
import io.jsonwebtoken.JwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(AppException.NotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of(
                "status", 404, "error", "Not Found", "message", ex.getMessage()));
    }

    @ExceptionHandler(AppException.ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(AppException.ConflictException ex) {
        return ResponseEntity.status(409).body(Map.of(
                "status", 409, "error", "Conflict", "message", ex.getMessage()));
    }

    @ExceptionHandler(AppException.UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(AppException.UnauthorizedException ex) {
        return ResponseEntity.status(401).body(Map.of(
                "status", 401, "error", "Unauthorized", "message", ex.getMessage()));
    }

    @ExceptionHandler(AppException.BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(AppException.BadRequestException ex) {
        return ResponseEntity.status(400).body(Map.of(
                "status", 400, "error", "Bad Request", "message", ex.getMessage()));
    }

    @ExceptionHandler(AppException.TooManyRequestsException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyRequests(AppException.TooManyRequestsException ex) {
        return ResponseEntity.status(429).body(Map.of(
                "status", 429, "error", "Too Many Requests", "message", ex.getMessage()));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Map<String, Object>> handleJwtException(JwtException ex) {
        return ResponseEntity.status(401).body(Map.of(
                "status",  401,
                "error",   "Unauthorized",
                "message", "Invalid or expired token. Please log in again."
        ));
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientAuth(
            InsufficientAuthenticationException ex) {
        return ResponseEntity.status(401).body(Map.of(
                "status",  401,
                "error",   "Unauthorized",
                "message", "Authentication required. Please provide a valid token."
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex) {
        return ResponseEntity.status(403).body(Map.of(
                "status",  403,
                "error",   "Forbidden",
                "message", "You do not have permission to access this resource."
        ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(401).body(Map.of(
                "status", 401, "error", "Unauthorized", "message", "Invalid email or password"));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabled(DisabledException ex) {
        return ResponseEntity.status(403).body(Map.of(
                "status", 403, "error", "Forbidden",
                "message", "Account not verified. Check your email."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(400).body(Map.of(
                "status", 400, "error", "Validation Failed", "message", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(500).body(Map.of(
                "status", 500, "error", "Internal Server Error",
                "message", "An unexpected error occurred"));
    }
}