package com.akmz.springBase.menu.model.dto;

import com.akmz.springBase.admin.model.entity.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuResponse {
    private Long menuId; // resourceId
    private String menuName;
    private String menuUrl;
    private String iconName;
    private Long parentId; // parentResourceId
    private Integer displayOrder;
    private Boolean isGroup;
    private List<MenuResponse> children = new ArrayList<>();

    // Resource 엔티티를 MenuResponse DTO로 변환하는 정적 메서드
    public static MenuResponse fromEntity(Resource resource) {
        return MenuResponse.builder()
                .menuId(resource.getResourceId())
                .menuName(resource.getMenuName())
                .menuUrl(resource.getMenuUrl())
                .iconName(resource.getIconName())
                .parentId(resource.getParentResourceId())
                .displayOrder(resource.getDisplayOrder())
                .isGroup(resource.getIsGroup())
                .children(new ArrayList<>()) // children은 재귀적으로 채워야 함
                .build();
    }
}
