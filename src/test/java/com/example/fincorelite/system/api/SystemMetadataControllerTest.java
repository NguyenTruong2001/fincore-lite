package com.example.fincorelite.system.api;

import com.example.fincorelite.shared.error.ErrorHttpMapping;
import com.example.fincorelite.shared.error.GlobalExceptionHandler;
import com.example.fincorelite.shared.error.ValidationViolationFactory;
import com.example.fincorelite.shared.web.correlation.CorrelationIdConstants;
import com.example.fincorelite.shared.web.correlation.CorrelationIdFilter;
import com.example.fincorelite.shared.web.correlation.CorrelationIdPolicy;
import com.example.fincorelite.shared.web.correlation.HttpRequestCorrelationIdAccessor;
import com.example.fincorelite.shared.web.correlation.UuidCorrelationIdGenerator;
import com.example.fincorelite.system.application.SystemMetadataApplicationService;
import com.example.fincorelite.system.domain.SystemMetadataAlreadyExistsException;
import com.example.fincorelite.system.domain.SystemMetadataNotFoundException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test này tồn tại vì {@code GlobalExceptionHandlerTest} gọi thẳng method của
 * handler, nên KHÔNG kiểm chứng được những thứ chỉ xuất hiện khi đi qua tầng
 * Spring MVC thật:
 *
 * <ul>
 *   <li>{@code @RestControllerAdvice} có được đăng ký hay không</li>
 *   <li>Content-Type có đúng {@code application/problem+json} hay không</li>
 *   <li>{@code Instant} serialize thành ISO-8601 hay epoch millis</li>
 *   <li>{@code HttpMessageNotReadableException} — ném ở tầng message
 *       converter, trước cả khi handler method được gọi — có thật sự bị bắt
 *       hay không</li>
 * </ul>
 *
 * <p>{@code @WebMvcTest} tự scan {@code @Controller}, {@code @ControllerAdvice}
 * và {@code Filter} — nên {@link GlobalExceptionHandler} và
 * {@link CorrelationIdFilter} vào context mà không cần khai báo. Ngược lại,
 * {@code @Component} thường KHÔNG được nạp, nên các dependency của chúng phải
 * {@code @Import} tường minh; thiếu một cái là context fail ngay lúc dựng.
 *
 * <p>Không đưa {@code FixedClockConfiguration} vào {@code @Import}: nested
 * {@code @TestConfiguration} đã được Spring Boot tự nhận. Import thêm sẽ đăng
 * ký {@code @Bean clock()} hai lần cùng tên và ném
 * {@code BeanDefinitionOverrideException}.
 */
@WebMvcTest(SystemMetadataController.class)
@Import({
        ErrorHttpMapping.class,
        ValidationViolationFactory.class,
        CorrelationIdPolicy.class,
        UuidCorrelationIdGenerator.class,
        HttpRequestCorrelationIdAccessor.class
})
class SystemMetadataControllerTest {

    private static final String BASE_PATH = "/api/v1/system-metadata";
    private static final String KEY = "database.foundation.version";

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        Clock clock() {
            return Clock.fixed(
                    Instant.parse("2026-08-07T10:15:30Z"),
                    ZoneOffset.UTC
            );
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemMetadataApplicationService applicationService;

    // -----------------------------------------------------------------
    // Happy path
    // -----------------------------------------------------------------

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        void createsMetadataAndReturnsCreatedWithLocation() throws Exception {
            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "metadataKey": "database.foundation.version",
                                      "metadataValue": "1"
                                    }
                                    """))
                   .andExpect(status().isCreated())
                   .andExpect(header().string(
                           "Location",
                           "http://localhost" + BASE_PATH + "/" + KEY
                   ))
                   .andExpect(jsonPath("$.metadataKey").value(KEY))
                   .andExpect(jsonPath("$.metadataValue").value("1"));

            then(applicationService).should().create(KEY, "1");
        }

        @Test
        void returnsMetadataValue() throws Exception {
            given(applicationService.getValue(KEY)).willReturn("1");

            mockMvc.perform(get(BASE_PATH + "/" + KEY))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.metadataKey").value(KEY))
                   .andExpect(jsonPath("$.metadataValue").value("1"));
        }

        @Test
        void updatesMetadataAndReturnsNoContent() throws Exception {
            mockMvc.perform(put(BASE_PATH + "/" + KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"metadataValue": "2"}
                                    """))
                   .andExpect(status().isNoContent());

            then(applicationService).should().changeValue(KEY, "2");
        }

        /**
         * Correlation ID do client gửi phải được giữ nguyên và vọng lại ở cả
         * response header lẫn body lỗi — đây là thứ dùng để nối log giữa các
         * service ở Sprint 7.
         */
        @Test
        void echoesClientCorrelationIdOnSuccess() throws Exception {
            given(applicationService.getValue(KEY)).willReturn("1");

            mockMvc.perform(get(BASE_PATH + "/" + KEY)
                            .header(CorrelationIdConstants.HEADER_NAME, "client-trace-01"))
                   .andExpect(status().isOk())
                   .andExpect(header().string(
                           CorrelationIdConstants.HEADER_NAME,
                           "client-trace-01"
                   ));
        }
    }

    // -----------------------------------------------------------------
    // Error path
    // -----------------------------------------------------------------

    @Nested
    @DisplayName("error path")
    class ErrorPath {

        @Test
        void mapsBlankKeyToValidationProblem() throws Exception {
            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "metadataKey": "",
                                      "metadataValue": "1"
                                    }
                                    """))
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentTypeCompatibleWith(
                           MediaType.APPLICATION_PROBLEM_JSON
                   ))
                   .andExpect(jsonPath("$.errorCode").value("COMMON_VALIDATION_FAILED"))
                   .andExpect(jsonPath("$.errors[0].field").value("metadataKey"))
                   .andExpect(jsonPath("$.errors[0].code").value("NotBlank"))
                   .andExpect(jsonPath("$.instance").value(BASE_PATH));

            then(applicationService).should(never()).create(anyString(), anyString());
        }

        @Test
        void mapsNotFoundToProblemDetail() throws Exception {
            given(applicationService.getValue(KEY))
                    .willThrow(new SystemMetadataNotFoundException(KEY));

            mockMvc.perform(get(BASE_PATH + "/" + KEY))
                   .andExpect(status().isNotFound())
                   .andExpect(content().contentTypeCompatibleWith(
                           MediaType.APPLICATION_PROBLEM_JSON
                   ))
                   .andExpect(jsonPath("$.errorCode").value("SYSTEM_METADATA_NOT_FOUND"))
                   .andExpect(jsonPath("$.title").value("System metadata not found"))
                   .andExpect(jsonPath("$.correlationId").exists());
        }

        @Test
        void mapsAlreadyExistsToConflict() throws Exception {
            willThrow(new SystemMetadataAlreadyExistsException(KEY, new RuntimeException("duplicate key")))
                    .given(applicationService)
                    .create(anyString(), anyString());

            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "metadataKey": "database.foundation.version",
                                      "metadataValue": "1"
                                    }
                                    """))
                   .andExpect(status().isConflict())
                   .andExpect(jsonPath("$.errorCode").value("SYSTEM_METADATA_ALREADY_EXISTS"));
        }

        @Test
        void mapsOptimisticLockingToConflict() throws Exception {
            willThrow(new OptimisticLockingFailureException("stale version"))
                    .given(applicationService)
                    .changeValue(anyString(), anyString());

            mockMvc.perform(put(BASE_PATH + "/" + KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"metadataValue": "2"}
                                    """))
                   .andExpect(status().isConflict())
                   .andExpect(jsonPath("$.errorCode").value("COMMON_CONCURRENT_MODIFICATION"));
        }
    }

    // -----------------------------------------------------------------
    // Edge case
    // -----------------------------------------------------------------

    @Nested
    @DisplayName("edge case")
    class EdgeCase {

        /**
         * Jackson ném HttpMessageNotReadableException TRƯỚC khi handler method
         * chạy. Message gốc của nó chứa tên class Jackson và vị trí byte trong
         * JSON, tuyệt đối không được lọt ra client.
         */
        @Test
        void doesNotLeakJacksonInternalsOnMalformedJson() throws Exception {
            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.errorCode").value("COMMON_MALFORMED_REQUEST"))
                   .andExpect(jsonPath("$.detail")
                           .value("Request body is missing or malformed"));
        }

        /**
         * Nếu Jackson serialize Instant thành epoch millis (khi thiếu
         * JavaTimeModule hoặc bật WRITE_DATES_AS_TIMESTAMPS), test này fail.
         * Đây chính là loại lỗi mà unit test gọi thẳng handler không bắt được.
         */
        @Test
        void serialisesTimestampAsIso8601() throws Exception {
            given(applicationService.getValue(KEY))
                    .willThrow(new SystemMetadataNotFoundException(KEY));

            mockMvc.perform(get(BASE_PATH + "/" + KEY))
                   .andExpect(status().isNotFound())
                   .andExpect(jsonPath("$.timestamp").value("2026-08-07T10:15:30Z"));
        }

        @Test
        void rejectsUnsupportedMediaType() throws Exception {
            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("not json"))
                   .andExpect(status().isUnsupportedMediaType())
                   .andExpect(jsonPath("$.errorCode")
                           .value("COMMON_UNSUPPORTED_MEDIA_TYPE"));
        }

        @Test
        void rejectsUnsupportedHttpMethod() throws Exception {
            mockMvc.perform(put(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                   .andExpect(status().isMethodNotAllowed())
                   .andExpect(jsonPath("$.errorCode")
                           .value("COMMON_METHOD_NOT_ALLOWED"));
        }

        /**
         * Client gửi correlation ID chứa khoảng trắng: phải bị loại và thay
         * bằng ID mới, không được vọng nguyên văn vào log/response
         * (log poisoning).
         */
        @Test
        void replacesInvalidClientCorrelationId() throws Exception {
            given(applicationService.getValue(KEY)).willReturn("1");

            mockMvc.perform(get(BASE_PATH + "/" + KEY)
                            .header(CorrelationIdConstants.HEADER_NAME, "has space"))
                   .andExpect(status().isOk())
                   .andExpect(header().string(
                           CorrelationIdConstants.HEADER_NAME,
                           Matchers.not("has space")
                   ));
        }

        @Test
        void mapsUnexpectedExceptionToInternalServerErrorWithoutLeakingDetail() throws Exception {
            given(applicationService.getValue(anyString()))
                    .willThrow(new IllegalStateException("connection pool exhausted at HikariPool-1"));

            mockMvc.perform(get(BASE_PATH + "/" + KEY))
                   .andExpect(status().isInternalServerError())
                   .andExpect(jsonPath("$.errorCode").value("COMMON_INTERNAL_ERROR"))
                   .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
        }
    }
}
