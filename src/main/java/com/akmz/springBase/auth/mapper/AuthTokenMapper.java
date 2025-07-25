package com.akmz.springBase.auth.mapper;

import com.akmz.springBase.auth.model.entity.AuthToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthTokenMapper {
    void insertAuthToken(AuthToken authToken);
    AuthToken findByToken(@Param("token") String token);
    void updateAuthTokenUsed(@Param("token") String token);
    void invalidateOldTokens(@Param("userName") String userName, @Param("tokenType") String tokenType);
    AuthToken findLatestTokenByUserNameAndType(@Param("userName") String userName, @Param("tokenType") String tokenType);
}
