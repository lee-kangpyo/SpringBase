package com.akmz.springBase.auth.model.dto;

import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@Schema(description = "비밀번호 재설정 요청 DTO")
public class PasswordResetRequest {
    @NotBlank(message = "사용자 ID는 필수입니다.")
    @Schema(description = "비밀번호 재설정을 요청할 사용자 ID (이메일 또는 사용자명)", example = "user@example.com", required = true)
    private String userId;
}
