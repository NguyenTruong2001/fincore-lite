package com.example.fincorelite.shared.error;

import org.springframework.http.HttpStatus;

import java.util.Objects;

public record ErrorHttpDescriptor(HttpStatus status, String title) {
    public ErrorHttpDescriptor {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(title, "title must not be null");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
    }
}
