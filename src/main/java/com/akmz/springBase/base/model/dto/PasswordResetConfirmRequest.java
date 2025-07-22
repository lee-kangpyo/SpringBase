package com.akmz.springBase.base.model.dto;

import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@Schema(description = "비밀번호 재설정 확인 요청 DTO")
public class PasswordResetConfirmRequest {
    @NotBlank(message = "토큰은 필수입니다.")
    @Schema(description = "비밀번호 재설정 토큰", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef", required = true)
    private String token;

    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @Size(min = 8, message = "새 비밀번호는 최소 8자 이상이어야 합니다.")
    @Schema(description = "새로운 비밀번호 (최소 8자 이상)", example = "newPassword123!", required = true)
    private String newPassword;
}
