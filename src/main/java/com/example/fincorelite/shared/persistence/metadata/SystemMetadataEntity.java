package com.example.fincorelite.shared.persistence.metadata;

import com.example.fincorelite.shared.persistence.audit.MutableAuditEntity;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.util.Objects;

@Entity
@Table(
        name = "system_metadata",
        schema = "fincore"
)
public class SystemMetadataEntity
        extends MutableAuditEntity
        implements Persistable<String> {

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

    @Transient
    private boolean newEntity = true;

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

    @Override
    public String getId() {
        return metadataKey;
    }

    @Override
    public boolean isNew() {
        return newEntity;
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

    @PostLoad
    @PostPersist
    private void markNotNew() {
        newEntity = false;
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