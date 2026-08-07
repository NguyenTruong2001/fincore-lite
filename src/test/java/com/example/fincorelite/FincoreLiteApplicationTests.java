package com.example.fincorelite;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration test: cần một PostgreSQL đang chạy (docker compose up -d)
 * vì {@code ddl-auto: validate} và Flyway đều yêu cầu schema thật.
 *
 * <p>Vì vậy test này bị loại khỏi {@code mvn test} qua
 * {@code excludedGroups=integration} và chỉ chạy khi bật profile
 * {@code integration-test}:
 *
 * <pre>./mvnw verify -Pintegration-test</pre>
 *
 * <p>TODO (Sprint sau): thay bằng Testcontainers + {@code @ServiceConnection}
 * để test tự dựng PostgreSQL và chạy được trong CI mà không cần DB dựng sẵn.
 */
@Tag("integration")
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("local")
class FincoreLiteApplicationTests {

    /**
     * Bắt được: sai tên bean trong {@code @EnableJpaAuditing}, entity không khớp
     * schema Flyway, bean circular dependency, thiếu datasource config.
     */
    @Test
    void contextLoads() {
    }
}
