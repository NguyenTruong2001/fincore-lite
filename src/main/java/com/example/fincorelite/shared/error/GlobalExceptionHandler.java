package com.example.fincorelite.shared.error;

import com.example.fincorelite.shared.web.correlation.CorrelationIdAccessor;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;


@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class
            );

    private static final String GLOBAL_FIELD =
            "_global";

    private final Clock clock;
    private final CorrelationIdAccessor correlationIdAccessor;
    private final ErrorHttpMapping errorHttpMapping;

    public GlobalExceptionHandler(
            Clock clock,
            CorrelationIdAccessor correlationIdAccessor,
            ErrorHttpMapping errorHttpMapping
    ) {
        this.clock = Objects.requireNonNull(
                clock,
                "clock must not be null"
        );

        this.correlationIdAccessor =
                Objects.requireNonNull(
                        correlationIdAccessor,
                        "correlationIdAccessor must not be null"
                );

        this.errorHttpMapping =
                Objects.requireNonNull(
                        errorHttpMapping,
                        "errorHttpMapping must not be null"
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<FieldViolation> errors =
                exception.getBindingResult()
                         .getAllErrors()
                         .stream()
                         .map(this::toFieldViolation)
                         .sorted(
                                 Comparator
                                         .comparing(
                                                 FieldViolation::field
                                         )
                                         .thenComparing(
                                                 FieldViolation::message
                                         )
                         )
                         .toList();

        return buildProblemDetail(
                ErrorCode.COMMON_VALIDATION_FAILED,
                "One or more request fields are invalid",
                request,
                errors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMalformedRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return buildProblemDetail(
                ErrorCode.COMMON_MALFORMED_REQUEST,
                "Request body is missing or malformed",
                request,
                null
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        return buildProblemDetail(
                exception.getErrorCode(),
                exception.getMessage(),
                request,
                null
        );
    }

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

        return buildProblemDetail(
                ErrorCode.COMMON_INTERNAL_ERROR,
                "An unexpected error occurred",
                request,
                null
        );
    }

    private FieldViolation toFieldViolation(
            ObjectError error
    ) {
        String field = GLOBAL_FIELD;

        if (error instanceof FieldError fieldError) {
            field = fieldError.getField();
        }

        String message = error.getDefaultMessage();

        if (message == null || message.isBlank()) {
            message = "invalid value";
        }

        return new FieldViolation(
                field,
                message
        );
    }

    private ResponseEntity<ProblemDetail> buildProblemDetail(
            ErrorCode errorCode,
            String detail,
            HttpServletRequest request,
            List<FieldViolation> errors
    ) {
        ErrorHttpDescriptor descriptor =
                errorHttpMapping.resolve(errorCode);

        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(
                        descriptor.status(),
                        detail
                );

        problemDetail.setType(
                URI.create("about:blank")
        );

        problemDetail.setTitle(
                descriptor.title()
        );

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

        problemDetail.setProperty(
                "correlationId",
                correlationIdAccessor.current()
        );

        if (errors != null && !errors.isEmpty()) {
            problemDetail.setProperty(
                    "errors",
                    errors
            );
        }

        return ResponseEntity
                .status(descriptor.status())
                .contentType(
                        MediaType.APPLICATION_PROBLEM_JSON
                )
                .body(problemDetail);
    }
}
