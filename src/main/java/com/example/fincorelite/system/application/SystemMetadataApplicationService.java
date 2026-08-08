package com.example.fincorelite.system.application;

import com.example.fincorelite.system.domain.SystemMetadataAlreadyExistsException;
import com.example.fincorelite.system.domain.SystemMetadataEntity;
import com.example.fincorelite.system.domain.SystemMetadataNotFoundException;
import com.example.fincorelite.system.infrastructure.SystemMetadataRepository;
import org.springframework.dao.DataIntegrityViolationException;
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

    /**
     * Uniqueness được enforce bằng primary key của database, KHÔNG bằng
     * {@code existsById} rồi mới {@code save}.
     *
     * <p>Check-then-act không an toàn dưới concurrency:
     *
     * <pre>
     * T1: existsById("k") -> false
     * T2: existsById("k") -> false
     * T1: INSERT "k"      -> OK
     * T2: INSERT "k"      -> vi phạm PK
     * </pre>
     *
     * <p>Cần {@code saveAndFlush} chứ không phải {@code save}: {@code save}
     * chỉ đưa entity vào persistence context, INSERT thật sự chạy lúc flush ở
     * cuối transaction — tức là SAU khi ra khỏi block {@code try} này, nên
     * exception sẽ thoát ra ngoài dưới dạng khác và không map được về 409.
     *
     * <p>Sau khi bắt được exception, transaction đã hỏng và không thể làm thêm
     * thao tác DB nào nữa. Ở đây chấp nhận được vì chúng ta ném ngay
     * {@link SystemMetadataAlreadyExistsException} và toàn bộ transaction
     * rollback.
     */
    @Transactional
    public void create(String metadataKey, String metadataValue) {
        SystemMetadataEntity entity = new SystemMetadataEntity(metadataKey, metadataValue);

        try {
            repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new SystemMetadataAlreadyExistsException(metadataKey, exception);
        }
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
