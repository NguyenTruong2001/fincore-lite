package com.example.fincorelite.shared.web.correlation;

public final class CorrelationIdConstants {
    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String REQUEST_ATTRIBUTE = CorrelationIdConstants.class.getName() + ".value";
    public static final String MDC_KEY = "correlationId";
    public static final int MAX_LENGTH = 128;

    private CorrelationIdConstants() {
        throw new AssertionError("CorrelationIdConstants must not be instantiated");
    }
}
