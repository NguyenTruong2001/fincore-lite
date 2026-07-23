package com.example.fincorelite.shared.persistence.identifier;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public final class RandomUuidIdentifierGenerator implements UuidIdentifierGenerator {
    @Override
    public UUID generate() {
        return UUID.randomUUID();
    }
}
