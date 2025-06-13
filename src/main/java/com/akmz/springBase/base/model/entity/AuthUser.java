package com.akmz.springBase.base.model.entity;

import lombok.Data;

@Data
public class AuthUser {
    private String userName;
    private String password;
    private String refreshToken;
    private String role;
    private String useYn;
}