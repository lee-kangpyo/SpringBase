package com.akmz.springBase.base.config;

import com.akmz.springBase.base.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final XssFilter xssFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", // 스웨거
                                "/api/auth/login", "/api/auth/token/reissue", "/api/auth/password/reset/**", "/css/**", "/js/**", "/h2-console/**").permitAll()
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())  // H2 콘솔 접근 허용
                )
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(xssFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        // 인증 실패 처리 (401 Unauthorized) 토큰 만료가 아닌 모든 401은 로그인 화면으로 이동
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

                            ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);

                            Object exception = request.getAttribute("exception");
                            if ("TOKEN_EXPIRED".equals(exception)) {
                                problemDetail.setTitle("Expired Token");
                                problemDetail.setDetail("엑세스 토큰이 만료되었습니다. 토큰을 refresh 하세요.");
                            } else {
                                problemDetail.setTitle("Unauthorized");
                                problemDetail.setDetail("인증에 실패했습니다. 자격 증명이 잘못되었거나 없습니다.");
                            }

                            objectMapper.writeValue(response.getOutputStream(), problemDetail);
                        })
                        // 인가 실패 처리 (403 Forbidden)
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

                            ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
                            problemDetail.setTitle("Forbidden");
                            problemDetail.setDetail("이 리소스에 접근할 권한이 없습니다..");

                            objectMapper.writeValue(response.getOutputStream(), problemDetail);
                        })
                )
                .userDetailsService(customUserDetailsService)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
//        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
