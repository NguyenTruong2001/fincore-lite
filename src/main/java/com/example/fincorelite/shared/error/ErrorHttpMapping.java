package com.example.fincorelite.shared.error;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public final class ErrorHttpMapping {

    public ErrorHttpDescriptor resolve(
            ErrorCode errorCode
    ) {
        Objects.requireNonNull(
                errorCode,
                "errorCode must not be null"
        );

        return switch (errorCode) {
            case COMMON_VALIDATION_FAILED ->
                    descriptor(
                            HttpStatus.BAD_REQUEST,
                            "Request validation failed",
                            "One or more request fields are invalid"
                    );

            case COMMON_MALFORMED_REQUEST ->
                    descriptor(
                            HttpStatus.BAD_REQUEST,
                            "Malformed request",
                            "Request body is missing or malformed"
                    );

            case COMMON_TYPE_MISMATCH ->
                    descriptor(
                            HttpStatus.BAD_REQUEST,
                            "Invalid parameter type",
                            "One or more request parameters have an invalid type"
                    );

            case COMMON_RESOURCE_NOT_FOUND ->
                    descriptor(
                            HttpStatus.NOT_FOUND,
                            "Resource not found",
                            "The requested resource was not found"
                    );

            case SYSTEM_METADATA_NOT_FOUND ->
                    descriptor(
                            HttpStatus.NOT_FOUND,
                            "System metadata not found",
                            "The requested system metadata was not found"
                    );

            case COMMON_DATA_CONFLICT ->
                    descriptor(
                            HttpStatus.CONFLICT,
                            "Conflict",
                            "The request conflicts with the current resource state"
                    );

            case SYSTEM_METADATA_ALREADY_EXISTS ->
                    descriptor(
                            HttpStatus.CONFLICT,
                            "System metadata already exists",
                            "A metadata entry with the same key already exists"
                    );

            case COMMON_CONCURRENT_MODIFICATION ->
                    descriptor(
                            HttpStatus.CONFLICT,
                            "Concurrent modification",
                            "The resource was modified by another operation"
                    );

            case COMMON_DATABASE_BUSY ->
                    descriptor(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "Service temporarily unavailable",
                            "The database operation could not be completed at this time"
                    );

            case COMMON_PERSISTENCE_INVARIANT_VIOLATED ->
                    descriptor(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Internal persistence error",
                            "An internal persistence invariant was violated"
                    );

            case COMMON_INTERNAL_ERROR ->
                    descriptor(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Internal server error",
                            "An unexpected error occurred"
                    );
        };
    }

    private static ErrorHttpDescriptor descriptor(
            HttpStatus status,
            String title,
            String detail
    ) {
        return new ErrorHttpDescriptor(
                status,
                title,
                detail
        );
    }
}