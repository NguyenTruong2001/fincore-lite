package com.example.fincorelite.shared.persistence.identifier;

import java.util.UUID;

@FunctionalInterface
public interface UuidIdentifierGenerator {

    UUID generate();
}
