package com.akmz.springBase.base.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Schema(description = "로그인 요청 정보")
public class LogoutRequest {
    @Schema(description = "사용자 이름", example = "user", required = true)
    @NotBlank
    private String userName;

    @Schema(description = "리프레쉬 토큰", required = true)
    @NotBlank
    private String refreshToken;
}
