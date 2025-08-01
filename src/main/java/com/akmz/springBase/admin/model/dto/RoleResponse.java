package com.akmz.springBase.admin.model.dto;

import com.akmz.springBase.admin.model.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "역할 응답 DTO")
public class RoleResponse {
    @Schema(description = "역할 ID", example = "1")
    private Long roleId;
    @Schema(description = "역할 이름 (예: ROLE_ADMIN)", example = "ROLE_USER")
    private String roleName;
    @Schema(description = "역할 설명", example = "일반 사용자 권한")
    private String description;

    public static RoleResponse fromEntity(Role role) {
        RoleResponse response = new RoleResponse();
        response.setRoleId(role.getRoleId());
        response.setRoleName(role.getRoleName());
        response.setDescription(role.getDescription());
        return response;
    }
}
