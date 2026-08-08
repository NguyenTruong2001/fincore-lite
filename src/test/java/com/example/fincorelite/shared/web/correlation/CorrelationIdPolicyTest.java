package com.example.fincorelite.shared.web.correlation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorrelationIdPolicyTest {

    private static final String GENERATED = "generated-correlation-id";

    private final CorrelationIdPolicy policy =
            new CorrelationIdPolicy(() -> GENERATED);

    @Nested
    @DisplayName("resolve - happy path")
    class Accepted {

        @ParameterizedTest
        @ValueSource(strings = {
                "abc123",
                "0",
                "a.b_c-d:e",
                "9F2E4C7A-1234",
                "trace_id.segment:01-99"
        })
        void keepsValidCandidate(String candidate) {
            assertThat(policy.resolve(candidate))
                    .isEqualTo(candidate);
        }

        @Test
        void keepsCandidateOfExactlyMaxLength() {
            String candidate = "a".repeat(
                    CorrelationIdConstants.MAX_LENGTH
            );

            assertThat(candidate).hasSize(128);
            assertThat(policy.resolve(candidate))
                    .isEqualTo(candidate);
        }
    }

    @Nested
    @DisplayName("resolve - error path")
    class Rejected {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
                "   ",
                "has space",
                "-starts-with-dash",
                ".starts-with-dot",
                "has/slash",
                "has\nnewline",
                "has;semicolon",
                "<script>"
        })
        void generatesNewIdForInvalidCandidate(String candidate) {
            assertThat(policy.resolve(candidate))
                    .isEqualTo(GENERATED);
        }

        @Test
        void generatesNewIdWhenCandidateExceedsMaxLength() {
            String candidate = "a".repeat(
                    CorrelationIdConstants.MAX_LENGTH + 1
            );

            assertThat(policy.resolve(candidate))
                    .isEqualTo(GENERATED);
        }

        @Test
        void failsFastWhenGeneratorProducesInvalidId() {
            CorrelationIdPolicy brokenPolicy =
                    new CorrelationIdPolicy(() -> "not valid!");

            assertThatThrownBy(() -> brokenPolicy.resolve(null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Generated correlation ID is invalid");
        }
    }

    @Nested
    @DisplayName("constructor")
    class Construction {

        @Test
        void rejectsNullGenerator() {
            assertThatThrownBy(() -> new CorrelationIdPolicy(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("generator must not be null");
        }
    }
}
