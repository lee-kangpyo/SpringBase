package com.akmz.springBase.auth.model.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
@Schema(description = "토큰 응답 객체")
public class TokenResponse {

    @Schema(description = "유저이름")
    private String userName;

    @Schema(description = "엑세스토큰")
    private String accessToken;

    @Schema(description = "리프레쉬토큰")
    private String refreshToken;
}
