package com.example.fincorelite.shared.web.correlation;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.regex.Pattern;

@Component
public class CorrelationIdPolicy {
    private static final Pattern VALID_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$");
    private final CorrelationIdGenerator generator;

    public CorrelationIdPolicy(CorrelationIdGenerator generator) {
        this.generator = Objects.requireNonNull(generator, "generator must not be null");
    }

    public String resolve(String candidate) {
        if (isValid(candidate)) {
            return candidate;
        }
        String generatedCorrelationId = generator.generate();
        if (!isValid(generatedCorrelationId)) {
            throw new IllegalStateException("Generated correlation ID is invalid");
        }
        return generatedCorrelationId;
    }

    boolean isValid(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        if (candidate.length() > CorrelationIdConstants.MAX_LENGTH) {
            return false;
        }
        return VALID_PATTERN.matcher(candidate)
                            .matches();
    }
}
