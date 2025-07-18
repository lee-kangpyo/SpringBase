package com.akmz.springBase.base.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GONE) // 410 Gone
public class ExpiredResetTokenException extends RuntimeException {
    public ExpiredResetTokenException(String message) {
        super(message);
    }
}
