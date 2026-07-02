package com.crewmeister.fxservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error(HttpStatus.NOT_FOUND, ex.getMessage(), request));
    }

    @ExceptionHandler({
            BindException.class,
            ConstraintViolationException.class,
            HandlerMethodValidationException.class,
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiErrorResponse> handleValidationError(Exception ex, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(error(HttpStatus.BAD_REQUEST, validationMessage(ex), request));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        HttpStatusCode statusCode = ex.getStatusCode();
        return ResponseEntity.status(statusCode)
                .body(error(statusCode, responseStatusMessage(ex), request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request));
    }

    private ApiErrorResponse error(HttpStatusCode statusCode, String message, HttpServletRequest request) {
        return new ApiErrorResponse(
                statusCode.value(),
                reasonPhrase(statusCode),
                message,
                request.getRequestURI()
        );
    }

    private String reasonPhrase(HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return status == null ? "HTTP " + statusCode.value() : status.getReasonPhrase();
    }

    private String responseStatusMessage(ResponseStatusException ex) {
        return ex.getReason() == null ? reasonPhrase(ex.getStatusCode()) : ex.getReason();
    }

    private String validationMessage(Exception ex) {
        if (ex instanceof MissingServletRequestParameterException missingParameter) {
            return "Missing required parameter: " + missingParameter.getParameterName();
        }
        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            return "Invalid value for parameter: " + mismatch.getName();
        }
        return "Validation failed";
    }
}
