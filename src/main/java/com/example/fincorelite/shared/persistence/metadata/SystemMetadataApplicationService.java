package com.example.fincorelite.shared.persistence.metadata;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class SystemMetadataApplicationService {
    private final SystemMetadataRepository repository;

    public SystemMetadataApplicationService(SystemMetadataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Transactional(readOnly = true)
    public String getValue(String metadataKey) {
        return repository.findById(metadataKey)
                         .orElseThrow(() -> new SystemMetadataNotFoundException(metadataKey))
                         .getMetadataValue();
    }

    @Transactional
    public void changeValue(String metadataKey, String newValue) {
        SystemMetadataEntity entity = repository.findById(metadataKey)
                                                .orElseThrow(() -> new SystemMetadataNotFoundException(metadataKey));

        entity.changeValue(newValue);
    }

    @Transactional
    public void changeValueWithPessimisticLock(String metadataKey, String newValue) {
        SystemMetadataEntity entity = lock(metadataKey);
        entity.changeValue(newValue);
    }

    @Transactional
    public void changeTwoValueWithOrderedLocks(String firstKey, String firstValue, String secondKey,
                                               String secondValue) {
        if (Objects.equals(firstKey, secondKey)) {
            throw new IllegalArgumentException("Metadata keys must be different");
        }
        List<MetadataChange> orderedChanges = Stream.of(new MetadataChange(firstKey, firstValue),
                                                        new MetadataChange(secondKey, secondValue))
                                                    .sorted(Comparator.comparing(MetadataChange::key))
                                                    .toList();

        for (MetadataChange change : orderedChanges) {
            SystemMetadataEntity entity = lock(change.key);
            entity.changeValue(change.value);
        }
    }

    private SystemMetadataEntity lock(String metadataKey) {
        return repository.findByMetadataKeyForUpdate(metadataKey)
                         .orElseThrow(() -> new SystemMetadataNotFoundException(metadataKey));

    }

    private record MetadataChange(String key, String value) {

    }
}
