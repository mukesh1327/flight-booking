package com.cloudxplorer.authservice.exception;

import com.cloudxplorer.authservice.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                Instant.now().toString(),
                request.getRequestURI(),
                ex.getCode(),
                ex.getMessage(),
                correlationId(request));
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception path={} message={}", request.getRequestURI(), ex.getMessage(), ex);
        ErrorResponse body = new ErrorResponse(
                Instant.now().toString(),
                request.getRequestURI(),
                "INTERNAL_ERROR",
                "unexpected error",
                correlationId(request));
        return ResponseEntity.internalServerError().body(body);
    }

    private String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute("x-correlation-id");
        if (value instanceof String str && !str.isBlank()) {
            return str;
        }
        return UUID.randomUUID().toString();
    }
}
