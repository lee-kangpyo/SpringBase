package com.akmz.springBase.admin.model.dto;

import com.akmz.springBase.admin.model.entity.Resource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "리소스 응답 DTO")
public class ResourceResponse {
    @Schema(description = "리소스 ID", example = "1")
    private Long resourceId;
    @Schema(description = "리소스 타입 (API, MENU_ITEM)", example = "MENU_ITEM")
    private String resourceType;
    @Schema(description = "리소스 고유 키/패턴 (예: /api/users, sidebar:user_management)", example = "/admin/dashboard")
    private String resourcePattern;
    @Schema(description = "HTTP 메소드 (GET, POST 등, API 타입에만 해당)", example = "GET")
    private String httpMethod;
    @Schema(description = "리소스 설명", example = "관리자 대시보드 메뉴")
    private String description;

    // MENU_ITEM 타입에만 해당되는 추가 컬럼
    @Schema(description = "메뉴 표시 이름 (예: 사용자 관리)", example = "대시보드")
    private String menuName;
    @Schema(description = "프론트엔드 라우팅 URL (예: /admin/users)", example = "/admin/dashboard")
    private String menuUrl;
    @Schema(description = "메뉴 아이콘 이름 (예: FaUserCog, Dashboard)", example = "FaHome")
    private String iconName;
    @Schema(description = "부모 메뉴의 RESOURCE_ID (그룹 메뉴용)", example = "null")
    private Long parentResourceId;
    @Schema(description = "메뉴 표시 순서", example = "1")
    private Integer displayOrder;
    @Schema(description = "그룹 메뉴 여부 (하위 메뉴를 가질 수 있음)", example = "false")
    private Boolean isGroup;

    public static ResourceResponse fromEntity(Resource resource) {
        return ResourceResponse.builder()
                .resourceId(resource.getResourceId())
                .resourceType(resource.getResourceType())
                .resourcePattern(resource.getResourcePattern())
                .httpMethod(resource.getHttpMethod())
                .description(resource.getDescription())
                .menuName(resource.getMenuName())
                .menuUrl(resource.getMenuUrl())
                .iconName(resource.getIconName())
                .parentResourceId(resource.getParentResourceId())
                .displayOrder(resource.getDisplayOrder())
                .isGroup(resource.getIsGroup())
                .build();
    }
}