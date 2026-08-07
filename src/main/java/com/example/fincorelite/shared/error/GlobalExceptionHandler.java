package com.example.fincorelite.shared.error;

import com.example.fincorelite.shared.web.correlation.CorrelationIdAccessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

/**
 * Chuyển mọi exception thoát khỏi controller thành RFC 7807 {@link ProblemDetail}.
 *
 * <p>Nguyên tắc: chỉ {@link BusinessException} được phép đưa message của chính nó
 * ra ngoài, vì message đó do chúng ta tự soạn. Mọi exception kỹ thuật đều dùng
 * detail tĩnh lấy từ {@link ErrorHttpMapping} để không rò rỉ nội bộ ra client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final URI BLANK_TYPE =
            URI.create("about:blank");

    private final Clock clock;
    private final CorrelationIdAccessor correlationIdAccessor;
    private final ErrorHttpMapping errorHttpMapping;
    private final ValidationViolationFactory validationViolationFactory;

    public GlobalExceptionHandler(
            Clock clock,
            CorrelationIdAccessor correlationIdAccessor,
            ErrorHttpMapping errorHttpMapping,
            ValidationViolationFactory validationViolationFactory
    ) {
        this.clock = Objects.requireNonNull(
                clock,
                "clock must not be null"
        );

        this.correlationIdAccessor = Objects.requireNonNull(
                correlationIdAccessor,
                "correlationIdAccessor must not be null"
        );

        this.errorHttpMapping = Objects.requireNonNull(
                errorHttpMapping,
                "errorHttpMapping must not be null"
        );

        this.validationViolationFactory = Objects.requireNonNull(
                validationViolationFactory,
                "validationViolationFactory must not be null"
        );
    }

    // ---------------------------------------------------------------------
    // 400 - Validation
    // ---------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        return validationProblem(
                validationViolationFactory.from(exception),
                request
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleHandlerMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request
    ) {
        return validationProblem(
                validationViolationFactory.from(exception),
                request
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        return validationProblem(
                validationViolationFactory.from(exception),
                request
        );
    }

    // ---------------------------------------------------------------------
    // 400 - Request không đọc được
    // ---------------------------------------------------------------------

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMalformedRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        /*
         * Message gốc của Jackson lộ tên class và cấu trúc JSON nội bộ,
         * nên chỉ log ở mức debug và trả detail tĩnh cho client.
         */
        log.debug(
                "Malformed request body for {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        return problem(
                ErrorCode.COMMON_MALFORMED_REQUEST,
                null,
                request,
                null
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        FieldViolation violation = new FieldViolation(
                exception.getName(),
                "TypeMismatch",
                "Value has an invalid type"
        );

        return problem(
                ErrorCode.COMMON_TYPE_MISMATCH,
                null,
                request,
                List.of(violation)
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        FieldViolation violation = new FieldViolation(
                exception.getParameterName(),
                "Missing",
                "Parameter is required"
        );

        return problem(
                ErrorCode.COMMON_MISSING_PARAMETER,
                null,
                request,
                List.of(violation)
        );
    }

    // ---------------------------------------------------------------------
    // 404 / 405 / 415 - Routing
    // ---------------------------------------------------------------------

    @ExceptionHandler({
            NoResourceFoundException.class,
            NoHandlerFoundException.class
    })
    public ResponseEntity<ProblemDetail> handleNotFound(
            Exception exception,
            HttpServletRequest request
    ) {
        return problem(
                ErrorCode.COMMON_RESOURCE_NOT_FOUND,
                null,
                request,
                null
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        return problem(
                ErrorCode.COMMON_METHOD_NOT_ALLOWED,
                null,
                request,
                null
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {
        return problem(
                ErrorCode.COMMON_UNSUPPORTED_MEDIA_TYPE,
                null,
                request,
                null
        );
    }

    // ---------------------------------------------------------------------
    // 409 / 503 - Persistence
    // ---------------------------------------------------------------------

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLocking(
            OptimisticLockingFailureException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Optimistic locking failure for {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        return problem(
                ErrorCode.COMMON_CONCURRENT_MODIFICATION,
                null,
                request,
                null
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Data integrity violation for {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        return problem(
                ErrorCode.COMMON_DATA_CONFLICT,
                null,
                request,
                null
        );
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handlePessimisticLocking(
            PessimisticLockingFailureException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Pessimistic locking failure for {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        return problem(
                ErrorCode.COMMON_DATABASE_BUSY,
                null,
                request,
                null
        );
    }

    // ---------------------------------------------------------------------
    // Business
    // ---------------------------------------------------------------------

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        return problem(
                exception.getErrorCode(),
                exception.getMessage(),
                request,
                null
        );
    }

    // ---------------------------------------------------------------------
    // Fallback
    // ---------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unhandled exception for {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        return problem(
                ErrorCode.COMMON_INTERNAL_ERROR,
                null,
                request,
                null
        );
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private ResponseEntity<ProblemDetail> validationProblem(
            List<FieldViolation> violations,
            HttpServletRequest request
    ) {
        return problem(
                ErrorCode.COMMON_VALIDATION_FAILED,
                null,
                request,
                violations
        );
    }

    /**
     * @param detail message hiển thị cho client; truyền {@code null} để dùng
     *               detail tĩnh an toàn của {@link ErrorHttpMapping}.
     */
    private ResponseEntity<ProblemDetail> problem(
            ErrorCode errorCode,
            String detail,
            HttpServletRequest request,
            List<FieldViolation> violations
    ) {
        ErrorHttpDescriptor descriptor =
                errorHttpMapping.resolve(errorCode);

        String resolvedDetail =
                (detail == null || detail.isBlank())
                        ? descriptor.detail()
                        : detail;

        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(
                        descriptor.status(),
                        resolvedDetail
                );

        problemDetail.setType(BLANK_TYPE);
        problemDetail.setTitle(descriptor.title());
        problemDetail.setInstance(
                URI.create(request.getRequestURI())
        );

        problemDetail.setProperty(
                "errorCode",
                errorCode.name()
        );

        problemDetail.setProperty(
                "timestamp",
                clock.instant()
        );

        String correlationId = currentCorrelationId();

        if (correlationId != null) {
            problemDetail.setProperty(
                    "correlationId",
                    correlationId
            );
        }

        if (violations != null && !violations.isEmpty()) {
            problemDetail.setProperty(
                    "errors",
                    violations
            );
        }

        return ResponseEntity
                .status(descriptor.status())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemDetail);
    }

    /**
     * Correlation ID không được phép làm hỏng response lỗi.
     *
     * <p>Nếu exception xảy ra trước khi {@code CorrelationIdFilter} kịp gán
     * attribute, accessor sẽ ném {@link IllegalStateException}. Ném tiếp từ
     * trong exception handler sẽ biến mọi lỗi thành trang lỗi mặc định của
     * servlet container và mất luôn ProblemDetail, nên ở đây nuốt và trả null.
     */
    private String currentCorrelationId() {
        try {
            return correlationIdAccessor.current();
        } catch (RuntimeException exception) {
            log.warn(
                    "Correlation ID unavailable while building error response",
                    exception
            );
            return null;
        }
    }
}
