package com.example.fincorelite.shared.web.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Objects;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter
        extends OncePerRequestFilter {

    private final CorrelationIdPolicy policy;

    public CorrelationIdFilter(
            CorrelationIdPolicy policy
    ) {
        this.policy = Objects.requireNonNull(
                policy,
                "policy must not be null"
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String candidate =
                extractSingleHeaderValue(request);

        String correlationId =
                policy.resolve(candidate);

        request.setAttribute(
                CorrelationIdConstants.REQUEST_ATTRIBUTE,
                correlationId
        );

        /*
         * Set trước khi gọi downstream vì response có thể
         * bị commit hoặc downstream phát sinh exception.
         */
        response.setHeader(
                CorrelationIdConstants.HEADER_NAME,
                correlationId
        );

        String previousMdcValue = MDC.get(
                CorrelationIdConstants.MDC_KEY
        );

        try {
            MDC.put(
                    CorrelationIdConstants.MDC_KEY,
                    correlationId
            );

            filterChain.doFilter(request, response);
        } finally {
            restorePreviousMdcValue(previousMdcValue);
        }
    }

    private String extractSingleHeaderValue(
            HttpServletRequest request
    ) {
        Enumeration<String> values =
                request.getHeaders(
                        CorrelationIdConstants.HEADER_NAME
                );

        if (values == null || !values.hasMoreElements()) {
            return null;
        }

        String firstValue = values.nextElement();

        /*
         * Client gửi nhiều header Correlation ID:
         * không chọn ngẫu nhiên một giá trị,
         * trả null để policy sinh ID mới.
         */
        if (values.hasMoreElements()) {
            return null;
        }

        return firstValue;
    }

    private void restorePreviousMdcValue(
            String previousValue
    ) {
        if (previousValue == null) {
            MDC.remove(CorrelationIdConstants.MDC_KEY);
            return;
        }

        MDC.put(
                CorrelationIdConstants.MDC_KEY,
                previousValue
        );
    }
}
