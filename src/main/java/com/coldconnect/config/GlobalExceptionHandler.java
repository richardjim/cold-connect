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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            AppException.NotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of(
                "status", 404, "error", "Not Found", "message", ex.getMessage()));
    }

    @ExceptionHandler(AppException.ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(
            AppException.ConflictException ex) {
        return ResponseEntity.status(409).body(Map.of(
                "status", 409, "error", "Conflict", "message", ex.getMessage()));
    }

    @ExceptionHandler(AppException.UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(
            AppException.UnauthorizedException ex) {
        return ResponseEntity.status(401).body(Map.of(
                "status", 401, "error", "Unauthorized", "message", ex.getMessage()));
    }

    @ExceptionHandler(AppException.BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(
            AppException.BadRequestException ex) {
        return ResponseEntity.status(400).body(Map.of(
                "status", 400, "error", "Bad Request", "message", ex.getMessage()));
    }

    @ExceptionHandler(AppException.TooManyRequestsException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyRequests(
            AppException.TooManyRequestsException ex) {
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
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex) {
        return ResponseEntity.status(401).body(Map.of(
                "status",  401,
                "error",   "Unauthorized",
                "message", "Invalid credentials."
        ));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabled(DisabledException ex) {
        return ResponseEntity.status(403).body(Map.of(
                "status",  403,
                "error",   "Forbidden",
                "message", "Account is disabled. Please contact support."
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(400).body(Map.of(
                "status",  400,
                "error",   "Validation Failed",
                "message", errors
        ));
    }

    // Fixes #107 #108 #184 #185 #186 #191 #192 #379 #397 #398 #402
    // #466 #467 #468 #479 #480 #488 #494 #495 #514
    // Spring throws this when a path variable or request param can't be
    // converted to the expected type (e.g. "abc" for a Long hubId)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        String expected = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "valid format";
        return ResponseEntity.status(400).body(Map.of(
                "status",  400,
                "error",   "Bad Request",
                "message", "Invalid value '" + ex.getValue()
                        + "' for parameter '" + ex.getName()
                        + "'. Expected: " + expected
        ));
    }

    // Fixes #205 #220 #264 #267 #311 #312 #314 #321 #356
    // Spring throws this when JSON body can't be parsed
    // (e.g. passing "abc" for a numeric field like amount or kg)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(
            HttpMessageNotReadableException ex) {
        return ResponseEntity.status(400).body(Map.of(
                "status",  400,
                "error",   "Bad Request",
                "message", "Invalid request body. Please check your field types and values."
        ));
    }

    @ExceptionHandler(org.springframework.web.client.ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleNetworkError(
            org.springframework.web.client.ResourceAccessException ex) {
        return ResponseEntity.status(503).body(Map.of(
                "status",  503,
                "error",   "Service Unavailable",
                "message", "No internet connection. Please check your network and try again."
        ));
    }

    // Catch-all — must be last
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(500).body(Map.of(
                "status",  500,
                "error",   "Internal Server Error",
                "message", "An unexpected error occurred"
        ));
    }
}