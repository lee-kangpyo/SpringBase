package com.akmz.springBase.admin.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "역할 업데이트 요청 DTO")
public class RoleUpdateRequest {
    @Schema(description = "업데이트할 역할 설명", example = "업데이트된 매니저 권한")
    private String description;
}
