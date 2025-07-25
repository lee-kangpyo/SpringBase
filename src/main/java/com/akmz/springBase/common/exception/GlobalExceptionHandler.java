package com.akmz.springBase.common.exception;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.dao.DataAccessException;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.AccessDeniedException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Value("${debug.include-exception-details:false}")
    private boolean includeExceptionDetails;

    // 유효성 검사 오류 처리 로직
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "유효성 검사 오류가 발생했습니다.");
        problemDetail.setTitle("Validation Error");

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        problemDetail.setProperty("errors", errors);

        if (includeExceptionDetails) {
            problemDetail.setProperty("debugMessage", ex.getMessage());
        }
        return problemDetail;
    }

    // DataAccessException (모든 DB 접근 예외) 처리 로직
    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail handleDataAccessException(DataAccessException ex) {
        log.error("Data Access Exception: {}", ex.getMessage(), ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "데이터베이스 처리 중 오류가 발생했습니다. 관리자에게 문의하세요.");
        problemDetail.setTitle("Database Operation Error");
        if (includeExceptionDetails) {
            problemDetail.setProperty("debugMessage", ex.getMessage());
            // 필요하다면, ex.getRootCause().getMessage() 등으로 더 상세한 원인 제공
        }
        return problemDetail;
    }

    // AccessDeniedException 처리 로직
    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDeniedException(AccessDeniedException ex) throws AccessDeniedException {
        log.error("Access Denied Exception caught by GlobalExceptionHandler, rethrowing: {}", ex.getMessage(), ex);
        throw ex; // 예외를 다시 던져 Spring Security의 AccessDeniedHandler가 처리하도록 함
    }

    // 기타 예상치 못한 모든 예외를 처리하는 제네릭 핸들러
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "예상치 못한 오류가 발생했습니다. 관리자에게 문의하세요.");
        problemDetail.setTitle("Unexpected Error");
        if (includeExceptionDetails) {
            problemDetail.setProperty("debugMessage", ex.getMessage());
        }
        return problemDetail;
    }
}
