package com.shopflow.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends BusinessException {
    public InsufficientStockException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
