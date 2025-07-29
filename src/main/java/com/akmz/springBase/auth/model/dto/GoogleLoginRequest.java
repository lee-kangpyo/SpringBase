package com.akmz.springBase.auth.model.dto;

import lombok.Data;

@Data
public class GoogleLoginRequest {
    private String googleId;
    private String email;
    private String name;
}
