package com.akmz.springBase.admin.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "메뉴 리소스 요청 DTO")
public class MenuResourceRequest {
    @Schema(description = "리소스 고유 키/패턴 (예: /admin/dashboard)", example = "/admin/dashboard", required = true)
    private String resourcePattern;
    @Schema(description = "HTTP 메소드 (API_ENDPOINT 타입일 경우만 해당)", example = "GET")
    private String httpMethod;
    @Schema(description = "리소스 설명", example = "관리자 대시보드 메뉴")
    private String description;
    @Schema(description = "메뉴 표시 이름 (예: 사용자 관리)", example = "대시보드", required = true)
    private String menuName;
    @Schema(description = "프론트엔드 라우팅 URL (예: /admin/users)", example = "/admin/dashboard", required = true)
    private String menuUrl;
    @Schema(description = "메뉴 아이콘 이름 (예: FaUserCog, Dashboard)", example = "FaHome")
    private String iconName;
    @Schema(description = "부모 메뉴의 RESOURCE_ID (그룹 메뉴용)", example = "null")
    private Long parentResourceId;
    @Schema(description = "메뉴 표시 순서", example = "1")
    private Integer displayOrder;
    @Schema(description = "그룹 메뉴 여부 (하위 메뉴를 가질 수 있음)", example = "false")
    private Boolean isGroup;
    @Schema(description = "사용 여부 (Y: 사용, N: 미사용)", example = "Y")
    private String useYn;
}
