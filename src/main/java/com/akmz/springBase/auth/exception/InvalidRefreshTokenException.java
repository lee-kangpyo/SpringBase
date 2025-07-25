package com.akmz.springBase.auth.exception;

// 리프레쉬 토큰이 유효하지 않음
public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}