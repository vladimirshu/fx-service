package com.crewmeister.fxservice.exception;

public record ApiErrorResponse(
        int status,
        String error,
        String message,
        String path
) {
}
