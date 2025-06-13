package com.akmz.springBase.base.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Schema(description = "로그인 요청 정보")
public class LoginRequest {

    @Schema(description = "사용자 이름", example = "user", required = true)
    @NotBlank
    private String userName;

    @Schema(description = "비밀번호", example = "user", required = true)
    @NotBlank
    private String password;
}
