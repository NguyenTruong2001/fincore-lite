package com.example.fincorelite.shared.error;


import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Component
public final class ValidationViolationFactory {

    private static final String GLOBAL_FIELD =
            "_global";

    private static final Comparator<FieldViolation>
            ORDERING =
            Comparator.comparing(
                    FieldViolation::field
            ).thenComparing(
                    FieldViolation::code
            ).thenComparing(
                    FieldViolation::message
            );

    public List<FieldViolation> from(
            MethodArgumentNotValidException exception
    ) {
        Stream<FieldViolation> fieldViolations =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(this::fromFieldError);

        Stream<FieldViolation> globalViolations =
                exception.getBindingResult()
                        .getGlobalErrors()
                        .stream()
                        .map(this::fromObjectError);

        return normalize(
                Stream.concat(
                        fieldViolations,
                        globalViolations
                ).toList()
        );
    }

    public List<FieldViolation> from(
            HandlerMethodValidationException exception
    ) {
        return normalize(
                exception
                        .getParameterValidationResults()
                        .stream()
                        .flatMap(result ->
                                result.getResolvableErrors()
                                        .stream()
                                        .map(error ->
                                                new FieldViolation(
                                                        parameterName(
                                                                result
                                                        ),
                                                        firstCode(
                                                                error.getCodes()
                                                        ),
                                                        safeMessage(
                                                                error.getDefaultMessage()
                                                        )
                                                )
                                        )
                        )
                        .toList()
        );
    }

    public List<FieldViolation> from(
            ConstraintViolationException exception
    ) {
        return normalize(
                exception.getConstraintViolations()
                        .stream()
                        .map(this::fromConstraintViolation)
                        .toList()
        );
    }

    private FieldViolation fromFieldError(
            FieldError error
    ) {
        return new FieldViolation(
                error.getField(),
                firstCode(error.getCodes()),
                safeMessage(
                        error.getDefaultMessage()
                )
        );
    }

    private FieldViolation fromObjectError(
            ObjectError error
    ) {
        return new FieldViolation(
                GLOBAL_FIELD,
                firstCode(error.getCodes()),
                safeMessage(
                        error.getDefaultMessage()
                )
        );
    }

    private FieldViolation fromConstraintViolation(
            ConstraintViolation<?> violation
    ) {
        return new FieldViolation(
                leafProperty(
                        violation.getPropertyPath()
                                .toString()
                ),
                violation
                        .getConstraintDescriptor()
                        .getAnnotation()
                        .annotationType()
                        .getSimpleName(),
                safeMessage(
                        violation.getMessage()
                )
        );
    }

    private static String parameterName(
            ParameterValidationResult result
    ) {
        MethodParameter parameter =
                result.getMethodParameter();

        RequestParam requestParam =
                parameter.getParameterAnnotation(
                        RequestParam.class
                );

        if (requestParam != null) {
            String explicitName = firstNonBlank(
                    requestParam.name(),
                    requestParam.value()
            );

            if (explicitName != null) {
                return explicitName;
            }
        }

        PathVariable pathVariable =
                parameter.getParameterAnnotation(
                        PathVariable.class
                );

        if (pathVariable != null) {
            String explicitName = firstNonBlank(
                    pathVariable.name(),
                    pathVariable.value()
            );

            if (explicitName != null) {
                return explicitName;
            }
        }

        RequestHeader requestHeader =
                parameter.getParameterAnnotation(
                        RequestHeader.class
                );

        if (requestHeader != null) {
            String explicitName = firstNonBlank(
                    requestHeader.name(),
                    requestHeader.value()
            );

            if (explicitName != null) {
                return explicitName;
            }
        }

        String parameterName =
                parameter.getParameterName();

        return parameterName == null
                ? "argument"
                : parameterName;
    }

    private static String leafProperty(
            String path
    ) {
        int separator = path.lastIndexOf('.');

        return separator < 0
                ? path
                : path.substring(separator + 1);
    }

    private static String firstCode(
            String[] codes
    ) {
        if (codes == null || codes.length == 0) {
            return "Invalid";
        }

        String code = codes[codes.length - 1];
        int separator = code.lastIndexOf('.');

        return separator < 0
                ? code
                : code.substring(separator + 1);
    }

    private static String safeMessage(
            String message
    ) {
        return message == null || message.isBlank()
                ? "Invalid value"
                : message;
    }

    private static String firstNonBlank(
            String first,
            String second
    ) {
        if (first != null && !first.isBlank()) {
            return first;
        }

        if (second != null && !second.isBlank()) {
            return second;
        }

        return null;
    }

    private static List<FieldViolation> normalize(
            List<FieldViolation> violations
    ) {
        return violations.stream()
                .distinct()
                .sorted(ORDERING)
                .toList();
    }
}