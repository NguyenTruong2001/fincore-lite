package com.example.fincorelite.shared.error;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ErrorHttpMapping {
    public ErrorHttpDescriptor resolve(ErrorCode errorCode) {
        Objects.requireNonNull(errorCode, "errorCode must be not null");
        return switch (errorCode) {
            case COMMON_VALIDATION_FAILED ->
                    new ErrorHttpDescriptor(HttpStatus.BAD_REQUEST, "Request validation failed");
            case COMMON_MALFORMED_REQUEST -> new ErrorHttpDescriptor(HttpStatus.BAD_REQUEST, "Malformed request");
            case COMMON_RESOURCE_NOT_FOUND -> new ErrorHttpDescriptor(HttpStatus.BAD_REQUEST, "Resource not found");
            case COMMON_CONFLICT -> new ErrorHttpDescriptor(HttpStatus.CONFLICT, "Conflict");
            case COMMON_INTERNAL_ERROR ->
                    new ErrorHttpDescriptor(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        };
    }
}
