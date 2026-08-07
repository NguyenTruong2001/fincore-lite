package com.example.fincorelite.shared.persistence.audit;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;
import java.util.Optional;

@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(
        auditorAwareRef = "auditorAware",
        dateTimeProviderRef = "auditingDateTimeProvider",
        modifyOnCreate = true
)
public class JpaAuditingConfiguration {

    private static final String SYSTEM_ACTOR = "system";
    private static final int MAX_ACTOR_LENGTH = 100;

    /**
     * Dùng {@link ObjectProvider} thay cho {@code @ConditionalOnMissingBean}.
     *
     * <p>{@code @ConditionalOnMissingBean} chỉ đảm bảo đúng thứ tự đánh giá bên
     * trong auto-configuration. Đặt nó trên một {@code @Bean} của user config thì
     * kết quả phụ thuộc thứ tự register bean — hôm nay chạy đúng, thêm một module
     * sau có thể sai âm thầm. {@code getIfAvailable} quyết định tại thời điểm
     * inject nên luôn xác định.
     */
    @Bean
    AuditorAware<String> auditorAware(
            ObjectProvider<CurrentActorProvider> currentActorProviders
    ) {
        CurrentActorProvider currentActorProvider =
                currentActorProviders.getIfAvailable(
                        () -> () -> SYSTEM_ACTOR
                );

        return () -> Optional.of(
                requireValidActor(currentActorProvider.currentActor())
        );
    }

    @Bean
    DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(clock.instant());
    }

    private static String requireValidActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalStateException(
                    "Current audit actor must not be blank"
            );
        }

        if (actor.length() > MAX_ACTOR_LENGTH) {
            throw new IllegalStateException(
                    "Current audit actor must not exceed "
                            + MAX_ACTOR_LENGTH
                            + " characters"
            );
        }

        return actor;
    }
}
