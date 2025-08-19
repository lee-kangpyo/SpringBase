package com.akmz.springBase.admin.model.entity;

import lombok.Data;

@Data
public class Resource {
    private Long resourceId;
    private String resourceType;
    private String resourcePattern;
    private String httpMethod;
    private String description;

    // MENU_ITEM 타입에만 해당되는 추가 컬럼
    private String menuName;
    private String menuUrl;
    private String iconName;
    private Long parentResourceId;
    private Integer displayOrder;
    private Boolean isGroup;
    private String useYn;
}
