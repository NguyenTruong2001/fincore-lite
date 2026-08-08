package com.example.fincorelite.shared.persistence.metadata;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SystemMetadataEntityTest {

    private static final int MAX_KEY_LENGTH = 100;
    private static final int MAX_VALUE_LENGTH = 500;

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        void createsEntityWithGivenKeyAndValue() {
            SystemMetadataEntity entity =
                    new SystemMetadataEntity("database.foundation.version", "1");

            assertThat(entity.getMetadataKey())
                    .isEqualTo("database.foundation.version");
            assertThat(entity.getMetadataValue()).isEqualTo("1");
            assertThat(entity.getId())
                    .isEqualTo("database.foundation.version");
        }

        /**
         * Entity dùng natural key nên Spring Data không thể suy ra "mới hay cũ"
         * từ id null. isNew() phải trả true trước khi persist, nếu không
         * save() sẽ thành merge và bắn thêm một SELECT thừa.
         */
        @Test
        void isNewBeforePersist() {
            SystemMetadataEntity entity =
                    new SystemMetadataEntity("k", "v");

            assertThat(entity.isNew()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "\t", "\n"})
        void rejectsBlankKey(String key) {
            assertThatThrownBy(() -> new SystemMetadataEntity(key, "v"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("metadataKey must not be blank");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "\t"})
        void rejectsBlankValue(String value) {
            assertThatThrownBy(() -> new SystemMetadataEntity("k", value))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("metadataValue must not be blank");
        }

        @Test
        void rejectsNullKey() {
            assertThatThrownBy(() -> new SystemMetadataEntity(null, "v"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("metadataKey must not be null");
        }

        @Test
        void rejectsNullValue() {
            assertThatThrownBy(() -> new SystemMetadataEntity("k", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("metadataValue must not be null");
        }
    }

    @Nested
    @DisplayName("length boundaries")
    class LengthBoundaries {

        @Test
        void acceptsKeyOfExactlyMaxLength() {
            String key = "k".repeat(MAX_KEY_LENGTH);

            assertThat(new SystemMetadataEntity(key, "v").getMetadataKey())
                    .hasSize(MAX_KEY_LENGTH);
        }

        @Test
        void rejectsKeyLongerThanColumn() {
            String key = "k".repeat(MAX_KEY_LENGTH + 1);

            assertThatThrownBy(() -> new SystemMetadataEntity(key, "v"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not exceed 100 characters");
        }

        @Test
        void acceptsValueOfExactlyMaxLength() {
            String value = "v".repeat(MAX_VALUE_LENGTH);

            assertThat(new SystemMetadataEntity("k", value).getMetadataValue())
                    .hasSize(MAX_VALUE_LENGTH);
        }

        @Test
        void rejectsValueLongerThanColumn() {
            String value = "v".repeat(MAX_VALUE_LENGTH + 1);

            assertThatThrownBy(() -> new SystemMetadataEntity("k", value))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not exceed 500 characters");
        }
    }

    @Nested
    @DisplayName("changeValue")
    class ChangeValue {

        @Test
        void updatesValue() {
            SystemMetadataEntity entity =
                    new SystemMetadataEntity("k", "old");

            entity.changeValue("new");

            assertThat(entity.getMetadataValue()).isEqualTo("new");
        }

        @Test
        void neverChangesKey() {
            SystemMetadataEntity entity =
                    new SystemMetadataEntity("k", "old");

            entity.changeValue("new");

            assertThat(entity.getMetadataKey()).isEqualTo("k");
        }

        @Test
        void rejectsBlankValue() {
            SystemMetadataEntity entity =
                    new SystemMetadataEntity("k", "old");

            assertThatThrownBy(() -> entity.changeValue("  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("metadataValue must not be blank");
        }

        @Test
        void rejectsValueLongerThanColumn() {
            SystemMetadataEntity entity =
                    new SystemMetadataEntity("k", "old");

            String tooLong = "v".repeat(MAX_VALUE_LENGTH + 1);

            assertThatThrownBy(() -> entity.changeValue(tooLong))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not exceed 500 characters");
        }

        /**
         * Invariant: khi validation fail, entity không được ở trạng thái nửa vời.
         */
        @Test
        void keepsPreviousValueWhenNewValueIsRejected() {
            SystemMetadataEntity entity =
                    new SystemMetadataEntity("k", "old");

            assertThatThrownBy(() -> entity.changeValue(""))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(entity.getMetadataValue()).isEqualTo("old");
        }
    }
}
