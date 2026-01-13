package org.amalitech.bloggingplatformspring.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException e) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ErrorResponse errorResponse = new ErrorResponse(
                "NOT FOUND",
                e.getMessage(),
                status.value()
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException e) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ErrorResponse errorResponse = new ErrorResponse(
                "BAD REQUEST",
                e.getMessage(),
                status.value()
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException e) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        ErrorResponse errorResponse = new ErrorResponse(
                "FORBIDDEN",
                e.getMessage(),
                status.value()
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(UserRegistrationException.class)
    public ResponseEntity<ErrorResponse> handleUserRegistrationException(UserRegistrationException e) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponse errorResponse = new ErrorResponse(
                "INTERNAL SERVER EXCEPTION",
                e.getMessage(),
                status.value()
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

}