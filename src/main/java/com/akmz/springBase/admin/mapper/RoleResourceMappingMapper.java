package com.akmz.springBase.admin.mapper;

import com.akmz.springBase.admin.model.entity.RoleResourceMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RoleResourceMappingMapper {
    List<RoleResourceMapping> findMappingsByRoleId(Long roleId);
    List<RoleResourceMapping> findMappingsByResourceId(Long resourceId);
    void insertMapping(RoleResourceMapping mapping);
    void deleteMapping(@Param("roleId") Long roleId, @Param("resourceId") Long resourceId);
}
