package com.xingang.community.common.exception;

import com.xingang.community.common.constant.ErrorCode;
import com.xingang.community.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Converts uncaught exceptions to the unified response structure.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex) {
        log.warn("business exception: code={}, message={}", ex.getCode(), ex.getMessage());
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Result<Void> handleResponseStatusException(ResponseStatusException ex) {
        String code = resolveStatusCode(ex);
        String message = ex.getReason() == null ? "request failed" : ex.getReason();
        log.warn("response status exception: code={}, message={}", code, message);
        return Result.fail(code, message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("invalid request parameter");
        return Result.fail(ErrorCode.PARAM_INVALID, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException ex) {
        return Result.fail(ErrorCode.PARAM_INVALID, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("unexpected error", ex);
        return Result.fail(ErrorCode.INTERNAL_ERROR, "system busy, please try again later");
    }

    private String resolveStatusCode(ResponseStatusException ex) {
        if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
            return ErrorCode.FORBIDDEN;
        }
        if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return ErrorCode.UNAUTHORIZED;
        }
        if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            return ErrorCode.TOO_MANY_REQUESTS;
        }
        return String.valueOf(ex.getStatusCode().value());
    }
}

