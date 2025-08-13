package org.example.eventsimulator;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ServerErrorDto> handleBind(WebExchangeBindException ex) {
        String details = ex.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ServerErrorDto body = new ServerErrorDto(
                "Validation failed for request body",
                details,
                LocalDateTime.now()
        );

        log.warn("400 Validation error: {}", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ServerErrorDto> handleConstraint(ConstraintViolationException ex) {
        String details = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining("; "));

        ServerErrorDto body = new ServerErrorDto(
                "Constraint violation",
                details,
                LocalDateTime.now()
        );

        log.warn("400 Constraint violation: {}", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ServerErrorDto> handleInput(ServerWebInputException ex) {
        String details = ex.getReason() != null ? ex.getReason() : "Invalid request";

        ServerErrorDto body = new ServerErrorDto(
                "Bad request",
                details,
                LocalDateTime.now()
        );

        log.warn("400 Bad input: {}", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ServerErrorDto> handleAny(Throwable ex) {
        ServerErrorDto body = new ServerErrorDto(
                "Internal Server Error",
                ex.getMessage() != null ? ex.getMessage() : "Unexpected error",
                LocalDateTime.now()
        );

        log.error("500 Internal error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}