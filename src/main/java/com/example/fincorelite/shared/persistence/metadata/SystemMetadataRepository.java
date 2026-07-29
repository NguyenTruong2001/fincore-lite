package com.example.fincorelite.shared.persistence.metadata;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemMetadataRepository extends JpaRepository<SystemMetadataEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT metadata
            FROM SystemMetadataEntity metadata
            WHERE metadata.metadataKey = :metadataKey
            """)
    Optional<SystemMetadataEntity> findByMetadataKeyForUpdate(
            @Param("metadataKey") String metadataKey
    );
}
