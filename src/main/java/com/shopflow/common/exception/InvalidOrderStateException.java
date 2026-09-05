package com.shopflow.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidOrderStateException extends BusinessException {
    public InvalidOrderStateException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
