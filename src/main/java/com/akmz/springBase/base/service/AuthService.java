package com.akmz.springBase.base.service;

import com.akmz.springBase.base.config.JwtTokenProvider;
import com.akmz.springBase.base.exception.InvalidRefreshTokenException;
import com.akmz.springBase.base.exception.RefreshTokenMismatchException;
import com.akmz.springBase.base.mapper.AuthMapper;
import com.akmz.springBase.base.model.dto.LoginRequest;
import com.akmz.springBase.base.model.dto.TokenResponse;
import com.akmz.springBase.base.model.entity.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        String username = request.getUserName();
        try {
            // Spring Security의 AuthenticationManager를 통해 인증 시도
            // CustomUserDetailsService.loadUserByUsername()가 호출되고,
            // 계정 잠금 여부 및 활성화 여부 등이 검증.
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );

            // 인증 성공 시: DB에 저장된 실패 횟수 초기화
            // 성공적인 로그인 시에만 실패 횟수 초기화
            loginSuccess(username);

            // UserDetails 객체에서 사용자 정보와 권한 가져오기
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            List<String> authorities = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).toList();

            // 토큰 발급
            String accessToken = jwtTokenProvider.createToken(userDetails.getUsername(), authorities);
            String refreshToken = jwtTokenProvider.createRefreshToken(userDetails.getUsername());

            saveRefreshToken(userDetails.getUsername(), refreshToken);

            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        } catch (LockedException | BadCredentialsException e) {
            // 계정 잠김, 비밀번호 불일치 시: 실패 횟수 증가 (잠긴 상태에서 시도해도 카운트 증가)
            loginFailure(username); // 실패 횟수 증가
            throw e;
        // UsernameNotFoundException 은 사용자 열거 공격을 막기 위해 스프링 내부적으로 BadCredentialsException 으로 변환해서 던짐
        } catch (UsernameNotFoundException e) { // 사용자 없음 (UsernameNotFoundException) 
            throw e;
        } catch (DisabledException e) { // 계정 비활성화 시 (DisabledException)
            throw e;
        } catch (Exception e) {
            // 그 외 알 수 없는 예외 처리 (매우 드뭄)
            throw new RuntimeException("알 수 없는 인증 오류 발생", e); // 일반 RuntimeException으로 래핑
        }
    }

    @Transactional
    public void loginSuccess(String username) {
        authMapper.resetLoginFailureCount(username); // DB 업데이트 (실패 횟수 0으로 초기화)
        System.out.println("Login success for user: " + username + ". Failed attempts reset.");
    }

    @Transactional
    public void loginFailure(String username) {
        // 사용자 존재 여부를 먼저 확인 (UserDetailsService에서 UsernameNotFoundException이 발생할 수도 있기 때문)
        AuthUser authUser = authMapper.findByUsername(username);
        if (authUser != null) {
            authMapper.updateLoginFailure(username); // DB 업데이트 (실패 횟수 증가)
            System.out.println("로그인 실패 (비밀번호 틀림) : " + username);
        } else {
            System.out.println("로그인 실패 (유저 정보가 존재하지 않음) : " + username);
        }
    }


    /**
     * 로그 아웃
     * @userId 해당하는 리프레쉬 토큰 삭제
     */
    public void logout(String userId) {
        saveRefreshToken(userId, null);
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