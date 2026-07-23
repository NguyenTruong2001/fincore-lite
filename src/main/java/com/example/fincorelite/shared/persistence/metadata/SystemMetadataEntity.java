package com.example.fincorelite.shared.persistence.metadata;

import com.example.fincorelite.shared.persistence.audit.MutableAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(
        name = "system_metadata",
        schema = "fincore"
)
public class SystemMetadataEntity
        extends MutableAuditEntity {

    private static final int MAX_KEY_LENGTH = 100;
    private static final int MAX_VALUE_LENGTH = 500;

    @Id
    @Column(
            name = "metadata_key",
            nullable = false,
            updatable = false,
            length = MAX_KEY_LENGTH
    )
    private String metadataKey;

    @Column(
            name = "metadata_value",
            nullable = false,
            length = MAX_VALUE_LENGTH
    )
    private String metadataValue;

    protected SystemMetadataEntity() {
    }

    public SystemMetadataEntity(
            String metadataKey,
            String metadataValue
    ) {
        this.metadataKey = requireText(
                metadataKey,
                "metadataKey",
                MAX_KEY_LENGTH
        );

        this.metadataValue = requireText(
                metadataValue,
                "metadataValue",
                MAX_VALUE_LENGTH
        );
    }

    public String getMetadataKey() {
        return metadataKey;
    }

    public String getMetadataValue() {
        return metadataValue;
    }

    public void changeValue(
            String newValue
    ) {
        metadataValue = requireText(
                newValue,
                "metadataValue",
                MAX_VALUE_LENGTH
        );
    }

    private static String requireText(
            String value,
            String fieldName,
            int maxLength
    ) {
        Objects.requireNonNull(
                value,
                fieldName + " must not be null"
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        if (value.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must not exceed "
                            + maxLength
                            + " characters"
            );
        }

        return value;
    }
}