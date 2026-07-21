package com.example.fincorelite.shared.web.correlation;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UuidCorrelationIdGenerator implements CorrelationIdGenerator {
    @Override
    public String generate() {
        return UUID.randomUUID()
                   .toString();
    }
}
