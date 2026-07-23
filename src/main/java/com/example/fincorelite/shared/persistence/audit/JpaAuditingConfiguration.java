package com.example.fincorelite.shared.persistence.audit;


import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;
import java.util.Optional;

@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "auditorDateTimeProvider",
        modifyOnCreate = true)
public class JpaAuditingConfiguration {

    private static final String SYSTEM_ACTOR = "system";
    private static final int MAX_ACTOR_LENGTH = 100;

    @Bean
    @ConditionalOnMissingBean(CurrentActorProvider.class)
    CurrentActorProvider systemCurrentActorProvider() {
        return () -> SYSTEM_ACTOR;
    }

    @Bean
    AuditorAware<String> auditorAware(CurrentActorProvider currentActorProvider) {
        return () -> Optional.of(requireValidActor(currentActorProvider.currentActor()));
    }

    @Bean
    DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(clock.instant());
    }

    private static String requireValidActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalStateException("Current audit actor must not be blank");
        }

        if (actor.length() > MAX_ACTOR_LENGTH) {
            throw new IllegalStateException("Current audit actor must not exceed " + MAX_ACTOR_LENGTH + " characters");
        }
        return actor;
    }
}
