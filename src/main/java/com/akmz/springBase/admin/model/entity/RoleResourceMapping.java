package com.akmz.springBase.admin.model.entity;

import lombok.Data;

@Data
public class RoleResourceMapping {
    private Long id;
    private Long roleId;
    private Long resourceId;
}
