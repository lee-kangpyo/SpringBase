package com.akmz.springBase.admin.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "관리자용 사용자 정보 응답 DTO")
public class UserAdminViewResponse {
    @Schema(description = "사용자명", example = "testuser")
    private String userName;
    @Schema(description = "이메일 주소", example = "test@example.com")
    private String email;
    @Schema(description = "사용 여부 (Y/N)", example = "Y")
    private String useYn;
    @Schema(description = "로그인 실패 횟수", example = "0")
    private int loginFailureCount;
    @Schema(description = "마지막 로그인 실패 시간", example = "2023-10-26T10:00:00")
    private LocalDateTime lastFailureTimestamp;
    @Schema(description = "사용자에게 할당된 역할 목록", example = "[\"ROLE_USER\", \"ROLE_ADMIN\"]")
    private List<String> roles;
}
