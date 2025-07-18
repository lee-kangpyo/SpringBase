package com.akmz.springBase.base.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegistrationRequest {

    @NotBlank(message = "사용자명을 입력해주세요.")
    @Size(min = 2, max = 20, message = "사용자명은 2자 이상 20자 이하로 입력해주세요.")
    @Pattern(regexp = "^[a-zA-Z0-9가-힣_]+$", message = "사용자명은 영문, 숫자, 한글, 언더스코어만 사용 가능합니다.")
    private String userName;

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식을 입력해주세요.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).+$", message = "비밀번호는 영문 대소문자와 숫자를 포함해야 합니다.")
    private String password;
}
