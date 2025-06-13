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

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AuthUser authUser = authMapper.findByUsername(username);

        if (authUser == null) {
            throw new UsernameNotFoundException("User not found");
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
                true,  // accountNonExpired
                true,  // credentialsNonExpired
                true,  // accountNonLocked
                authorities
        );
    }
}