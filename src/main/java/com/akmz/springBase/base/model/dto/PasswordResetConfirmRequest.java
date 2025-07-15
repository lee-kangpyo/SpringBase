package com.akmz.springBase.base.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetConfirmRequest {
    private String token;
    private String newPassword;
}
