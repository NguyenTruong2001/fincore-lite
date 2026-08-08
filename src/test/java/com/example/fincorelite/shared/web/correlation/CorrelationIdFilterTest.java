package com.example.fincorelite.shared.web.correlation;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private static final String GENERATED = "generated-correlation-id";
    private static final String HEADER =
            CorrelationIdConstants.HEADER_NAME;

    private CorrelationIdFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter(
                new CorrelationIdPolicy(() -> GENERATED)
        );

        request = new MockHttpServletRequest("GET", "/any");
        response = new MockHttpServletResponse();

        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void propagatesValidIncomingHeader() throws Exception {
        request.addHeader(HEADER, "client-trace-01");

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(HEADER))
                .isEqualTo("client-trace-01");

        assertThat(request.getAttribute(
                CorrelationIdConstants.REQUEST_ATTRIBUTE
        )).isEqualTo("client-trace-01");
    }

    @Test
    void generatesIdWhenHeaderAbsent() throws Exception {
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(HEADER))
                .isEqualTo(GENERATED);
    }

    @Test
    void generatesIdWhenHeaderIsInvalid() throws Exception {
        request.addHeader(HEADER, "has space");

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(HEADER))
                .isEqualTo(GENERATED);
    }

    /**
     * Edge case quan trọng: client gửi nhiều header cùng tên.
     * Chọn đại một giá trị là mở đường cho request smuggling / log poisoning,
     * nên filter phải bỏ cả hai và sinh ID mới.
     */
    @Test
    void generatesIdWhenHeaderIsSentMoreThanOnce() throws Exception {
        request.addHeader(HEADER, "first-value");
        request.addHeader(HEADER, "second-value");

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(HEADER))
                .isEqualTo(GENERATED)
                .isNotIn("first-value", "second-value");
    }

    @Test
    void exposesCorrelationIdToMdcDuringChainExecution() throws Exception {
        request.addHeader(HEADER, "client-trace-01");

        AtomicReference<String> mdcDuringChain =
                new AtomicReference<>();

        MockFilterChain capturingChain = new MockFilterChain() {
            @Override
            public void doFilter(
                    jakarta.servlet.ServletRequest req,
                    jakarta.servlet.ServletResponse res
            ) throws IOException, ServletException {
                mdcDuringChain.set(
                        MDC.get(CorrelationIdConstants.MDC_KEY)
                );
                super.doFilter(req, res);
            }
        };

        filter.doFilter(request, response, capturingChain);

        assertThat(mdcDuringChain.get())
                .isEqualTo("client-trace-01");
    }

    @Test
    void clearsMdcAfterRequest() throws Exception {
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(MDC.get(CorrelationIdConstants.MDC_KEY))
                .isNull();
    }

    /**
     * Thread trong pool có thể còn MDC sót từ request trước.
     * Filter phải khôi phục đúng giá trị cũ, không xóa trắng.
     */
    @Test
    void restoresPreviousMdcValueAfterRequest() throws Exception {
        MDC.put(CorrelationIdConstants.MDC_KEY, "pre-existing");

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(MDC.get(CorrelationIdConstants.MDC_KEY))
                .isEqualTo("pre-existing");
    }

    /**
     * Response header phải được set TRƯỚC khi gọi downstream,
     * nếu không thì request lỗi sẽ trả về không kèm correlation ID.
     */
    @Test
    void setsResponseHeaderEvenWhenDownstreamThrows() {
        MockFilterChain failingChain = new MockFilterChain() {
            @Override
            public void doFilter(
                    jakarta.servlet.ServletRequest req,
                    jakarta.servlet.ServletResponse res
            ) {
                throw new IllegalStateException("downstream failure");
            }
        };

        try {
            filter.doFilter(request, response, failingChain);
        } catch (Exception ignored) {
            // exception được rethrow là đúng, không phải nội dung test này
        }

        assertThat(response.getHeader(HEADER))
                .isEqualTo(GENERATED);

        assertThat(MDC.get(CorrelationIdConstants.MDC_KEY))
                .isNull();
    }
}
