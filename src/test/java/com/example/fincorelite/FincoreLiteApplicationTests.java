package com.example.fincorelite;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test: cần Docker đang chạy, không cần PostgreSQL dựng sẵn.
 *
 * <p>Chạy bằng:
 *
 * <pre>./mvnw verify -Pintegration-test</pre>
 *
 * <p>Bị loại khỏi {@code mvn test} qua {@code excludedGroups=integration} để
 * vòng lặp unit test giữ được tốc độ.
 *
 * <p>Profile {@code test} được dùng thay cho {@code local} một cách có chủ ý.
 * {@code application-local.yml} có sẵn datasource URL trỏ tới
 * {@code localhost:5432}; nếu test chạy với profile đó và
 * {@code @ServiceConnection} vì lý do nào đó không được áp dụng, Flyway sẽ
 * migrate thẳng vào database dev thật. Profile {@code test} không khai báo
 * datasource nào, nên container là nguồn duy nhất — sai là fail ngay chứ
 * không âm thầm phá dữ liệu.
 */
@Tag("integration")
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class FincoreLiteApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    /**
     * Bắt được: sai tên bean trong {@code @EnableJpaAuditing}, entity không
     * khớp schema Flyway ({@code ddl-auto: validate}), bean circular
     * dependency, thiếu datasource config.
     */
    @Test
    void contextLoads() {
    }
}
