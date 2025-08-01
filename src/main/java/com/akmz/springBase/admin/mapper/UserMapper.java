package com.akmz.springBase.admin.mapper;

import com.akmz.springBase.admin.model.dto.UserAdminViewResponse;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {
    List<UserAdminViewResponse> findAll();
    List<UserAdminViewResponse> findAllWithRoles();
}
