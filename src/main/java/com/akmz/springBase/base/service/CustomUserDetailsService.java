package com.akmz.springBase.base.service;

import com.akmz.springBase.base.mapper.AuthMapper;
import com.akmz.springBase.base.model.entity.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthMapper authMapper;

    // 잠금 시간 설정
    private static final long LOCK_TIME_MILLIS = 30 * 60 * 1000; // 30분

    // 최대 실패 횟수
    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AuthUser authUser = authMapper.findByUsername(username);

        if (authUser == null) {
            throw new UsernameNotFoundException("User not found");
        }
        
        boolean isAccountNonLocked = true; // 계정 잠김 여부 (잘못된 로그인이 여러번 반복되었을때)

        // 계정 잠금 체크 시작
        int currentFailureCount = authUser.getLoginFailureCount() != null ? authUser.getLoginFailureCount() : 0;

        if (currentFailureCount >= MAX_FAILED_ATTEMPTS) {
            // 실패 횟수가 최대치를 넘었을 경우 잠금 시간 확인
            if (authUser.getLastFailureTimestamp() == null) {
                // 이 경우는 발생해서는 안 되지만 (최대 실패 시 Timestamp가 있어야 함), 방어 코드로
                // 실패 횟수가 MAX를 넘었는데 타임스탬프가 없다면, 일단 잠기지 않은 것으로 간주 (혹은 즉시 잠금 처리 등 정책 필요)
                isAccountNonLocked = true;
            } else {
                long lastFailureTime = authUser.getLastFailureTimestamp().getTime();
                long currentTime = System.currentTimeMillis();

                if ((currentTime - lastFailureTime) < LOCK_TIME_MILLIS) {
                    // 잠금 시간이 아직 지나지 않았다면 계정 잠김 상태로 설정
                    isAccountNonLocked = false;
                    // 이 경우, UserDetails를 반환하면 Spring Security의 DaoAuthenticationProvider가 LockedException을 throw.
                } else {
                    // --- 정책 2 적용: 잠금 시간이 지났더라도, DB 실패 횟수를 여기서 초기화 X ---
                    // 초기화는 오직 성공적인 로그인 시 (AuthenticationSuccessHandler)에만 수행
                    // 따라서 계정은 잠겨있지 않은 상태이지만, 실패 횟수는 여전히 MAX_FAILED_ATTEMPTS 이상
                    // 이 상태에서 비밀번호를 다시 틀리면 즉시 재잠금
                    isAccountNonLocked = true;
                }
            }
        }

        // 권한 목록을 별도 쿼리로 가져오기
        List<String> roles = authMapper.getAuthoritiesByUsername(username);

        if (roles == null || roles.isEmpty()) {
            // 권한이 없으면 기본 ROLE_USER 부여 (필요에 따라 조정)
            roles = Collections.singletonList("ROLE_USER");
        }

        // String 리스트를 SimpleGrantedAuthority 리스트로 변환
        List<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
                authUser.getUserName(),
                authUser.getPassword(),
                "Y".equals(authUser.getUseYn()),
                true,    // accountNonExpired
                true,                   // credentialsNonExpired
                isAccountNonLocked,
                authorities
        );
    }
}