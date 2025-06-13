package com.akmz.springBase.base.exception;

// 사용자가 전달한 리프레쉬 토큰이 DB에 있는 리프레쉬 토큰과 일치하지 않음
public class RefreshTokenMismatchException extends RuntimeException {
  public RefreshTokenMismatchException(String message) {
    super(message);
  }
}