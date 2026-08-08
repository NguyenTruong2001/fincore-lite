package com.example.fincorelite.shared.error;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ErrorHttpMappingTest {

    private final ErrorHttpMapping mapping = new ErrorHttpMapping();

    /**
     * Chốt chặn quan trọng nhất của class này: thêm một ErrorCode mới mà quên
     * map sẽ làm switch không exhaustive (compile error), còn nếu map sai kiểu
     * thì test dưới đây bắt được ngay.
     */
    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void resolvesEveryErrorCodeToAnErrorStatus(ErrorCode errorCode) {
        ErrorHttpDescriptor descriptor = mapping.resolve(errorCode);

        assertThat(descriptor).isNotNull();
        assertThat(descriptor.title()).isNotBlank();
        assertThat(descriptor.detail()).isNotBlank();
        assertThat(descriptor.status().isError())
                .as("ErrorCode %s must map to a 4xx or 5xx status", errorCode)
                .isTrue();
    }

    @Test
    void mapsValidationFailureToBadRequest() {
        assertThat(mapping.resolve(ErrorCode.COMMON_VALIDATION_FAILED).status())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void mapsSystemMetadataNotFoundToNotFound() {
        assertThat(mapping.resolve(ErrorCode.SYSTEM_METADATA_NOT_FOUND).status())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void mapsSystemMetadataAlreadyExistsToConflict() {
        assertThat(mapping.resolve(ErrorCode.SYSTEM_METADATA_ALREADY_EXISTS).status())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void mapsConcurrentModificationToConflict() {
        assertThat(mapping.resolve(ErrorCode.COMMON_CONCURRENT_MODIFICATION).status())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void mapsInternalErrorToInternalServerError() {
        assertThat(mapping.resolve(ErrorCode.COMMON_INTERNAL_ERROR).status())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Detail tĩnh sẽ được trả thẳng cho client, nên không được chứa
     * tên class, tên bảng, hay bất kỳ chi tiết nội bộ nào.
     */
    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void staticDetailDoesNotLeakInternals(ErrorCode errorCode) {
        String detail = mapping.resolve(errorCode).detail();

        assertThat(detail)
                .doesNotContain("Exception")
                .doesNotContain("com.example")
                .doesNotContain("SQL")
                .doesNotContain("org.hibernate");
    }

    @Test
    void rejectsNullErrorCode() {
        assertThatThrownBy(() -> mapping.resolve(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("errorCode must not be null");
    }
}
