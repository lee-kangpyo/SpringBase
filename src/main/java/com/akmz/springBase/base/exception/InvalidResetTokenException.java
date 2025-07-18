package com.akmz.springBase.base.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 404 Not Found 또는 HttpStatus.GONE (410)
@ResponseStatus(HttpStatus.NOT_FOUND)
public class InvalidResetTokenException extends RuntimeException {
    public InvalidResetTokenException(String message) {
        super(message);
    }
}
