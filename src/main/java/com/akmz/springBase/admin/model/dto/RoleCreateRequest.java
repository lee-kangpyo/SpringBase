package com.akmz.springBase.admin.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "역할 생성 요청 DTO")
public class RoleCreateRequest {
    @Schema(description = "생성할 역할 이름 (예: ROLE_MANAGER)", example = "ROLE_MANAGER", required = true)
    private String roleName;
    @Schema(description = "역할 설명", example = "매니저 권한")
    private String description;
}
