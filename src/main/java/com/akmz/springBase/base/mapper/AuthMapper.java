package com.akmz.springBase.base.mapper;

import com.akmz.springBase.base.model.entity.AuthUser;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AuthMapper {
    AuthUser findByUsername(String username);

    List<String> getAuthoritiesByUsername(String username);

    void updateRefreshToken(AuthUser authUser);
}
