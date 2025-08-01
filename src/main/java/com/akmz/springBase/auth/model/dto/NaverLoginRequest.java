package com.akmz.springBase.auth.model.dto;

import lombok.Data;

@Data
public class NaverLoginRequest {
    private String naverId;
    private String email;
    private String name;
}
