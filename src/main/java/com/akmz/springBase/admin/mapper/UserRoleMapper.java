package com.akmz.springBase.admin.mapper;

import com.akmz.springBase.admin.model.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserRoleMapper {
    List<UserRole> findUserRolesByUserName(String userName);
    void insertUserRole(UserRole userRole);
    void deleteUserRole(@Param("userName") String userName, @Param("roleId") Long roleId);
}
