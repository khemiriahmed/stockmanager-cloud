package com.stockmanager.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BusinessException(String message) {
        this(message, "BUSINESS_ERROR", HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String message, String code) {
        this(message, code, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String message, String code, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(message, "NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(message, "CONFLICT", HttpStatus.CONFLICT);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(message, "FORBIDDEN", HttpStatus.FORBIDDEN);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(message, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
    }
}
