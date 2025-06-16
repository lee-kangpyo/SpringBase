package com.akmz.springBase.base.mapper;

import com.akmz.springBase.base.model.entity.AuthUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuthMapper {
    AuthUser findByUsername(@Param("userName") String userName);

    List<String> getAuthoritiesByUsername(String username);

    void updateRefreshToken(AuthUser authUser);

    // --- 추가: 로그인 실패 횟수 및 마지막 실패 시각 업데이트 메서드 ---
    void updateLoginFailure(@Param("userName") String userName); // 실패 시 카운트 증가 및 시각 업데이트
    void resetLoginFailureCount(@Param("userName") String userName); // 성공 시 카운트 초기화
    void resetLoginFailureOnUnlock(@Param("userName") String userName); // 잠금 해제 시 카운트 초기화
}
