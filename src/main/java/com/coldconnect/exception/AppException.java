package com.coldconnect.exception;

import org.springframework.http.HttpStatus;

public class AppException extends RuntimeException {

    private final HttpStatus status;

    protected AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    public static class NotFoundException extends AppException {
        public NotFoundException(String msg) { super(msg, HttpStatus.NOT_FOUND); }
    }

    public static class ConflictException extends AppException {
        public ConflictException(String msg) { super(msg, HttpStatus.CONFLICT); }
    }

    public static class UnauthorizedException extends AppException {
        public UnauthorizedException(String msg) { super(msg, HttpStatus.UNAUTHORIZED); }
    }

    public static class BadRequestException extends AppException {
        public BadRequestException(String msg) { super(msg, HttpStatus.BAD_REQUEST); }
    }

    public static class TooManyRequestsException extends AppException {
        public TooManyRequestsException(String msg) { super(msg, HttpStatus.TOO_MANY_REQUESTS); }
    }
}
