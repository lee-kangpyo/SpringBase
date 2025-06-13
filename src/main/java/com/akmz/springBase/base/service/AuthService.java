package com.akmz.springBase.base.service;

import com.akmz.springBase.base.config.JwtTokenProvider;
import com.akmz.springBase.base.exception.InvalidRefreshTokenException;
import com.akmz.springBase.base.exception.RefreshTokenMismatchException;
import com.akmz.springBase.base.mapper.AuthMapper;
import com.akmz.springBase.base.model.dto.LoginRequest;
import com.akmz.springBase.base.model.dto.LogoutRequest;
import com.akmz.springBase.base.model.dto.TokenResponse;
import com.akmz.springBase.base.model.entity.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthMapper authMapper;
    private final CustomUserDetailsService userDetailsService;

    /**
     * 로그인 api 토큰 발급 기능, db저장 기능 추가
     * @param request
     * @return
     */
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUserName(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String accessToken = jwtTokenProvider.createToken(userDetails.getUsername(),
                userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).toList());

        String refreshToken = jwtTokenProvider.createRefreshToken(userDetails.getUsername());

        saveRefreshToken(userDetails.getUsername(), refreshToken);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    /**
     * 로그 아웃
     * @param accessToken 엑세스 토큰에서 id 를 추출 후 삭제
     */
    public void logout(String accessToken) {
        String username = jwtTokenProvider.getUsername(accessToken);
        saveRefreshToken(username, null);
    }

    /**
     * 엑세스 토큰 재발급
     * @param refreshToken 리프레쉬 토큰
     * @return
     */
    public TokenResponse  refreshAccessToken(String refreshToken){
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        String username = jwtTokenProvider.getUsername(refreshToken);

        AuthUser authUser = authMapper.findByUsername(username);

        if (!refreshToken.equals(authUser.getRefreshToken())) {
            throw new RefreshTokenMismatchException("Refresh token does not match");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        String newAccessToken = jwtTokenProvider.createToken(
                userDetails.getUsername(),
                userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
        );

        // 새로운 리프레시 토큰도 생성
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userDetails.getUsername());

        // DB에 새로운 리프레시 토큰 저장 (기존 토큰 무효화)
        saveRefreshToken(userDetails.getUsername(), newRefreshToken);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    /**
     * 리프레쉬 토큰을 db에 저장하는 메서드
     * @param username 사용자이름 또는 아이디 같은 유니크한 키값
     * @param refreshToken 리프레쉬 토큰
     */
    private void saveRefreshToken(String username, String refreshToken) {
        AuthUser authUser = new AuthUser();
        authUser.setUserName(username);
        authUser.setRefreshToken(refreshToken);
        authMapper.updateRefreshToken(authUser);
    }

}