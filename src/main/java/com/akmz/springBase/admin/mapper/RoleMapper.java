package com.akmz.springBase.admin.mapper;

import com.akmz.springBase.admin.model.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface RoleMapper {
    List<Role> findAllRoles();
    Role findRoleById(Long roleId);
    Role findRoleByName(String roleName);
    void insertRole(Role role);
    void updateRole(Role role);
    void deleteRole(Long roleId);
}
