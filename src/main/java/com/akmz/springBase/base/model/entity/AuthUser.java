package com.akmz.springBase.base.model.entity;

import lombok.Data;

import java.util.Date;

@Data
public class AuthUser {
    private String userName;
    private String password;
    private String refreshToken;
    private String role;
    private String useYn;
    // --- 로그인 시도 제한 관련 필드 추가 ---
    private Integer loginFailureCount; // 로그인 실패 횟수
    private Date lastFailureTimestamp; // 마지막 로그인 실패 시각
}