package com.example.fincorelite.shared.persistence.audit;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

@MappedSuperclass
public abstract class MutableAuditEntity extends CreatedAuditEntity {

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected MutableAuditEntity() {
    }

    public final Instant getUpdatedAt() {
        return updatedAt;
    }

    public final String getUpdatedBy() {
        return updatedBy;
    }

    public final long getVersion() {
        return version;
    }
}
