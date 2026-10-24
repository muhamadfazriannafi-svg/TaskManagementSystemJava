package com.example.taskmanagement.advice;

import java.time.Instant;
import java.util.List;

/** Single response shape for every error the API produces. */
public record ApiError(Instant timestamp, int status, String error, String message, String path,
                       List<FieldError> details) {

    public record FieldError(String field, String message) {
    }
}
