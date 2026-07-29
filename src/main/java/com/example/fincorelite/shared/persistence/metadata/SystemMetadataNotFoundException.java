package com.example.fincorelite.shared.persistence.metadata;

public final class SystemMetadataNotFoundException extends RuntimeException {
    public SystemMetadataNotFoundException(String metadataKey) {
        super("System metadata not found: " + metadataKey);
    }
}
