package com.example.fincorelite.shared.error;

import com.example.fincorelite.shared.persistence.metadata.SystemMetadataAlreadyExistsException;
import com.example.fincorelite.shared.persistence.metadata.SystemMetadataNotFoundException;
import com.example.fincorelite.shared.web.correlation.CorrelationIdAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private static final Instant FIXED_INSTANT =
            Instant.parse("2026-08-07T10:15:30Z");

    private static final String CORRELATION_ID = "test-correlation-id";

    private MockHttpServletRequest request;
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("POST", "/api/v1/system-metadata");

        handler = newHandler(() -> CORRELATION_ID);
    }

    private static GlobalExceptionHandler newHandler(
            CorrelationIdAccessor accessor
    ) {
        return new GlobalExceptionHandler(
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC),
                accessor,
                new ErrorHttpMapping(),
                new ValidationViolationFactory()
        );
    }

    // -----------------------------------------------------------------
    // Business exception
    // -----------------------------------------------------------------

    /**
     * Regression test: trước đây SystemMetadataNotFoundException extends
     * RuntimeException nên rơi vào handler fallback và trả 500 thay vì 404.
     */
    @Test
    void mapsSystemMetadataNotFoundToNotFound() {
        ResponseEntity<ProblemDetail> response =
                handler.handleBusinessException(
                        new SystemMetadataNotFoundException("database.foundation.version"),
                        request
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(properties(response))
                .containsEntry("errorCode", "SYSTEM_METADATA_NOT_FOUND");
    }

    @Test
    void mapsSystemMetadataAlreadyExistsToConflict() {
        ResponseEntity<ProblemDetail> response =
                handler.handleBusinessException(
                        new SystemMetadataAlreadyExistsException("some.key", null),
                        request
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(properties(response))
                .containsEntry("errorCode", "SYSTEM_METADATA_ALREADY_EXISTS");
    }

    @Test
    void usesBusinessExceptionMessageAsDetail() {
        ResponseEntity<ProblemDetail> response =
                handler.handleBusinessException(
                        new BusinessException(
                                ErrorCode.COMMON_DATA_CONFLICT,
                                "Account is frozen"
                        ),
                        request
                );

        assertThat(body(response).getDetail())
                .isEqualTo("Account is frozen");
    }

    @Test
    void alwaysSetsProblemJsonContentType() {
        ResponseEntity<ProblemDetail> response =
                handler.handleBusinessException(
                        new SystemMetadataNotFoundException("k"),
                        request
                );

        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void includesCorrelationIdTimestampAndInstance() {
        ResponseEntity<ProblemDetail> response =
                handler.handleBusinessException(
                        new SystemMetadataNotFoundException("k"),
                        request
                );

        assertThat(properties(response))
                .containsEntry("correlationId", CORRELATION_ID)
                .containsEntry("timestamp", FIXED_INSTANT);

        assertThat(body(response).getInstance())
                .hasToString("/api/v1/system-metadata");
    }

    // -----------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------

    @Test
    void mapsValidationFailureToBadRequestWithFieldErrors() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.rejectValue(
                null,
                "NotBlank",
                "metadataValue must not be blank"
        );

        ResponseEntity<ProblemDetail> response =
                handler.handleMethodArgumentNotValid(
                        methodArgumentNotValid(bindingResult),
                        request
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(properties(response))
                .containsEntry("errorCode", "COMMON_VALIDATION_FAILED");

        @SuppressWarnings("unchecked")
        List<FieldViolation> errors =
                (List<FieldViolation>) properties(response).get("errors");

        assertThat(errors).isNotEmpty();
    }

    // -----------------------------------------------------------------
    // Persistence
    // -----------------------------------------------------------------

    @Test
    void mapsOptimisticLockingFailureToConflict() {
        ResponseEntity<ProblemDetail> response =
                handler.handleOptimisticLocking(
                        new OptimisticLockingFailureException("stale version"),
                        request
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(properties(response))
                .containsEntry("errorCode", "COMMON_CONCURRENT_MODIFICATION");
    }

    @Test
    void mapsDataIntegrityViolationToConflict() {
        ResponseEntity<ProblemDetail> response =
                handler.handleDataIntegrityViolation(
                        new DataIntegrityViolationException(
                                "duplicate key value violates unique constraint \"pk_system_metadata\""
                        ),
                        request
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    /**
     * Message của DataIntegrityViolationException chứa tên constraint và
     * đôi khi cả giá trị đang bị trùng. Không được đẩy ra client.
     */
    @Test
    void doesNotLeakPersistenceMessageToClient() {
        ResponseEntity<ProblemDetail> response =
                handler.handleDataIntegrityViolation(
                        new DataIntegrityViolationException(
                                "duplicate key value violates unique constraint \"pk_system_metadata\""
                        ),
                        request
                );

        assertThat(body(response).getDetail())
                .doesNotContain("pk_system_metadata")
                .doesNotContain("duplicate key");
    }

    // -----------------------------------------------------------------
    // Fallback
    // -----------------------------------------------------------------

    @Test
    void mapsUnexpectedExceptionToInternalServerErrorWithoutLeakingMessage() {
        ResponseEntity<ProblemDetail> response =
                handler.handleUnexpectedException(
                        new IllegalStateException(
                                "connection to 10.0.0.7:5432 refused"
                        ),
                        request
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        assertThat(body(response).getDetail())
                .isEqualTo("An unexpected error occurred")
                .doesNotContain("10.0.0.7");

        assertThat(properties(response))
                .containsEntry("errorCode", "COMMON_INTERNAL_ERROR");
    }

    @Test
    void mapsRoutingFailureToNotFound() {
        ResponseEntity<ProblemDetail> response =
                handler.handleNotFound(
                        new IllegalStateException("no handler"),
                        request
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(properties(response))
                .containsEntry("errorCode", "COMMON_RESOURCE_NOT_FOUND");
    }

    // -----------------------------------------------------------------
    // Resilience
    // -----------------------------------------------------------------

    /**
     * Nếu exception xảy ra trước khi CorrelationIdFilter kịp chạy, accessor sẽ
     * ném. Handler vẫn phải trả ProblemDetail hợp lệ, chỉ thiếu correlationId.
     */
    @Test
    void stillBuildsProblemDetailWhenCorrelationIdIsUnavailable() {
        GlobalExceptionHandler brokenHandler = newHandler(() -> {
            throw new IllegalStateException(
                    "Correlation ID not available in the current request"
            );
        });

        ResponseEntity<ProblemDetail> response =
                brokenHandler.handleUnexpectedException(
                        new RuntimeException("boom"),
                        request
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        assertThat(properties(response))
                .doesNotContainKey("correlationId");
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private static ProblemDetail body(
            ResponseEntity<ProblemDetail> response
    ) {
        ProblemDetail problemDetail = response.getBody();
        assertThat(problemDetail).isNotNull();
        return problemDetail;
    }

    private static Map<String, Object> properties(
            ResponseEntity<ProblemDetail> response
    ) {
        Map<String, Object> properties = body(response).getProperties();
        assertThat(properties).isNotNull();
        return properties;
    }

    private static MethodArgumentNotValidException methodArgumentNotValid(
            BeanPropertyBindingResult bindingResult
    ) throws NoSuchMethodException {
        MethodParameter parameter = new MethodParameter(
                Target.class.getDeclaredMethod("handle", String.class),
                0
        );

        return new MethodArgumentNotValidException(parameter, bindingResult);
    }

    @SuppressWarnings("unused")
    static class Target {
        void handle(String value) {
            // chỉ tồn tại để lấy MethodParameter
        }
    }
}
