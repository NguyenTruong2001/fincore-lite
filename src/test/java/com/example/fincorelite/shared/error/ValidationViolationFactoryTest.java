package com.example.fincorelite.shared.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationViolationFactoryTest {

    private final ValidationViolationFactory factory =
            new ValidationViolationFactory();

    // -----------------------------------------------------------------
    // MethodArgumentNotValidException (@Valid @RequestBody)
    // -----------------------------------------------------------------

    @Test
    void mapsFieldErrorToFieldViolation() throws Exception {
        BeanPropertyBindingResult bindingResult = bindingResult();
        bindingResult.rejectValue(
                "metadataValue",
                "NotBlank",
                "metadataValue must not be blank"
        );

        List<FieldViolation> violations =
                factory.from(methodArgumentNotValid(bindingResult));

        assertThat(violations).hasSize(1);
        assertThat(violations.getFirst().field())
                .isEqualTo("metadataValue");
        assertThat(violations.getFirst().code())
                .isEqualTo("NotBlank");
        assertThat(violations.getFirst().message())
                .isEqualTo("metadataValue must not be blank");
    }

    @Test
    void mapsGlobalErrorToUnderscoreGlobalField() throws Exception {
        BeanPropertyBindingResult bindingResult = bindingResult();
        bindingResult.reject(
                "KeyValueMismatch",
                "metadataKey and metadataValue must not be equal"
        );

        List<FieldViolation> violations =
                factory.from(methodArgumentNotValid(bindingResult));

        assertThat(violations).hasSize(1);
        assertThat(violations.getFirst().field()).isEqualTo("_global");
        assertThat(violations.getFirst().code()).isEqualTo("KeyValueMismatch");
    }

    /**
     * Thứ tự phải ổn định, nếu không thì response body đổi giữa các lần chạy
     * và không thể assert được trong test của client.
     */
    @Test
    void sortsViolationsByFieldThenCode() throws Exception {
        BeanPropertyBindingResult bindingResult = bindingResult();
        bindingResult.rejectValue("metadataValue", "Size", "too long");
        bindingResult.rejectValue("metadataKey", "NotBlank", "must not be blank");
        bindingResult.reject("GlobalRule", "global problem");

        List<FieldViolation> violations =
                factory.from(methodArgumentNotValid(bindingResult));

        assertThat(violations)
                .extracting(FieldViolation::field)
                .containsExactly("_global", "metadataKey", "metadataValue");
    }

    @Test
    void removesDuplicateViolations() throws Exception {
        BeanPropertyBindingResult bindingResult = bindingResult();
        bindingResult.rejectValue("metadataKey", "NotBlank", "must not be blank");
        bindingResult.rejectValue("metadataKey", "NotBlank", "must not be blank");

        List<FieldViolation> violations =
                factory.from(methodArgumentNotValid(bindingResult));

        assertThat(violations).hasSize(1);
    }

    @Test
    void fallsBackToDefaultMessageWhenMessageIsMissing() throws Exception {
        BeanPropertyBindingResult bindingResult = bindingResult();
        bindingResult.rejectValue("metadataKey", "NotBlank", null);

        List<FieldViolation> violations =
                factory.from(methodArgumentNotValid(bindingResult));

        assertThat(violations.getFirst().message())
                .isEqualTo("Invalid value");
    }

    @Test
    void returnsEmptyListWhenThereAreNoErrors() throws Exception {
        List<FieldViolation> violations =
                factory.from(methodArgumentNotValid(bindingResult()));

        assertThat(violations).isEmpty();
    }

    // -----------------------------------------------------------------
    // ConstraintViolationException (validation trên service / @Validated)
    // -----------------------------------------------------------------

    @Test
    void mapsConstraintViolationsUsingAnnotationNameAsCode() {
        ConstraintViolationException exception =
                constraintViolationException(new SampleBean("", "far-too-long"));

        List<FieldViolation> violations = factory.from(exception);

        assertThat(violations)
                .extracting(FieldViolation::field)
                .containsExactly("code", "metadataValue");

        assertThat(violations)
                .extracting(FieldViolation::code)
                .containsExactly("Size", "NotBlank");
    }

    @Test
    void usesLeafPropertyNameForNestedPaths() {
        ConstraintViolationException exception =
                constraintViolationException(new SampleBean("", "ok"));

        List<FieldViolation> violations = factory.from(exception);

        assertThat(violations)
                .extracting(FieldViolation::field)
                .doesNotContain("sampleBean.metadataValue");
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private static BeanPropertyBindingResult bindingResult() {
        return new BeanPropertyBindingResult(
                new SampleRequest(),
                "sampleRequest"
        );
    }

    private static MethodArgumentNotValidException methodArgumentNotValid(
            BeanPropertyBindingResult bindingResult
    ) throws NoSuchMethodException {
        MethodParameter parameter = new MethodParameter(
                TestController.class.getDeclaredMethod(
                        "handle",
                        SampleRequest.class
                ),
                0
        );

        return new MethodArgumentNotValidException(parameter, bindingResult);
    }

    private static ConstraintViolationException constraintViolationException(
            SampleBean bean
    ) {
        try (ValidatorFactory validatorFactory =
                     Validation.buildDefaultValidatorFactory()) {

            Validator validator = validatorFactory.getValidator();
            Set<ConstraintViolation<SampleBean>> violations =
                    validator.validate(bean);

            return new ConstraintViolationException(violations);
        }
    }

    @SuppressWarnings("unused")
    static class TestController {
        void handle(SampleRequest request) {
            // chỉ tồn tại để lấy MethodParameter
        }
    }

    @SuppressWarnings("unused")
    static class SampleRequest {

        private String metadataKey;
        private String metadataValue;

        public String getMetadataKey() {
            return metadataKey;
        }

        public String getMetadataValue() {
            return metadataValue;
        }
    }

    static class SampleBean {

        @NotBlank
        private final String metadataValue;

        @Size(max = 3)
        private final String code;

        SampleBean(String metadataValue, String code) {
            this.metadataValue = metadataValue;
            this.code = code;
        }

        public String getMetadataValue() {
            return metadataValue;
        }

        public String getCode() {
            return code;
        }
    }
}
