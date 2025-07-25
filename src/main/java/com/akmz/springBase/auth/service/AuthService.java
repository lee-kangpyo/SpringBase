package com.akmz.springBase.auth.service;

import com.akmz.springBase.common.config.JwtTokenProvider;
import com.akmz.springBase.auth.exception.ExpiredResetTokenException;
import com.akmz.springBase.auth.exception.InvalidRefreshTokenException;
import com.akmz.springBase.auth.exception.InvalidResetTokenException;
import com.akmz.springBase.auth.exception.RefreshTokenMismatchException;
import com.akmz.springBase.auth.mapper.AuthMapper;
import com.akmz.springBase.auth.mapper.AuthTokenMapper;
import com.akmz.springBase.notification.email.model.dto.EmailRequest;
import com.akmz.springBase.notification.email.service.EmailService;
import com.akmz.springBase.auth.model.dto.LoginRequest;
import com.akmz.springBase.auth.model.dto.TokenResponse;
import com.akmz.springBase.auth.model.dto.UserRegistrationRequest;
import com.akmz.springBase.auth.model.entity.AuthToken;
import com.akmz.springBase.auth.model.entity.AuthUser;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthMapper authMapper;
    private final CustomUserDetailsService userDetailsService;
    private final AuthTokenMapper authTokenMapper;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.base-url}")
    private String appBaseUrl;

    private static final long PASSWORD_RESET_TOKEN_EXPIRATION_MILLIS = 30 * 60 * 1000; // 30분
    private static final String TOKEN_TYPE_PASSWORD_RESET = "PASSWORD_RESET";

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
                    .userName(userDetails.getUsername())
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
            log.error("알 수 없는 인증 오류 발생", e);
            // 그 외 알 수 없는 예외 처리 (매우 드뭄)
            throw new RuntimeException("알 수 없는 인증 오류 발생", e);
        }
    }

    /**
     * 회원가입 처리
     * @param request 회원가입 요청 DTO
     * @throws com.akmz.springBase.auth.exception.UserAlreadyExistsException 이메일 또는 사용자명이 이미 존재할 경우
     */
    @Transactional
    public void registerUser(UserRegistrationRequest request) {
        // 1. 이메일 중복 검사
        if (authMapper.existsByEmail(request.getEmail())) {
            throw new com.akmz.springBase.auth.exception.UserAlreadyExistsException("이미 등록된 이메일입니다.");
        }

        // 2. 사용자명 중복 검사
        if (authMapper.existsByUserName(request.getUserName())) {
            throw new com.akmz.springBase.auth.exception.UserAlreadyExistsException("이미 사용 중인 사용자명입니다.");
        }

        // 3. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 4. AuthUser 엔티티 생성
        AuthUser newUser = new AuthUser();
        newUser.setUserName(request.getUserName());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(encodedPassword);
        newUser.setUseYn("Y"); // 기본적으로 사용 가능으로 설정
        newUser.setLoginFailureCount(0); // 로그인 실패 횟수 초기화

        // 5. 데이터베이스 저장
        authMapper.save(newUser);
        log.info("새로운 사용자 등록 완료: {}", request.getEmail());
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
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userDetails.getUsername());
        saveRefreshToken(username, newRefreshToken); // DB에 새로운 리프레시 토큰 저장

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken) // 새로 발급된 리프레시 토큰 반환
                .build();
    }

    /**
     * 리프레쉬 토큰을 db에 저장하는 메서드
     * @param username 사용자이름 또는 아이디 같은 유니크한 키값
     * @param refreshToken 리프레쉬 토큰
     */
    private void saveRefreshToken(String username, String refreshToken) {
        log.debug("Attempting to save refresh token for user: {}", username);
        AuthUser authUser = authMapper.findByUsername(username);
        if (authUser != null) {
            log.debug("Found user {}. Updating refresh token.", username);
            authUser.setRefreshToken(refreshToken);
            authMapper.updateRefreshToken(authUser);
            log.info("Refresh token updated for user {}: {}", username, refreshToken);
        } else {
            log.warn("사용자를 찾을 수 없습니다: {}", username);
        }
    }

    /**
     * 비밀번호 재설정 요청 처리
     * @param userId 재설정을 요청한 사용자의 이메일 (여기서는 userName)
     * @throws UsernameNotFoundException 해당 이메일로 등록된 사용자가 없을 경우
     * @throws MessagingException 이메일 전송 실패 시
     */
    @Transactional
    public void requestPasswordReset(String userId) throws MessagingException {
        AuthUser user = authMapper.findByUsername(userId);
        if (user == null) {
            throw new UsernameNotFoundException("해당 아이디로 등록된 사용자가 없습니다: " + userId);
        }

        // 기존의 해당 타입 토큰 무효화
        authTokenMapper.invalidateOldTokens(user.getUserName(), TOKEN_TYPE_PASSWORD_RESET);

        String token = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + PASSWORD_RESET_TOKEN_EXPIRATION_MILLIS);

        AuthToken authToken = new AuthToken();
        authToken.setToken(token);
        authToken.setUserName(user.getUserName());
        authToken.setTokenType(TOKEN_TYPE_PASSWORD_RESET);
        authToken.setExpiryDate(expiryDate);
        authToken.setCreatedDate(now);
        authToken.setUsed(false);

        authTokenMapper.insertAuthToken(authToken);

        String resetLink = appBaseUrl + "/reset-password?token=" + token;
        String subject = "비밀번호 재설정 링크";
        String text = "비밀번호를 재설정하려면 다음 링크를 클릭하세요: " + resetLink + "\n\n이 링크는 30분 동안 유효합니다.";

        EmailRequest emailRequest = EmailRequest.builder()
                .to(user.getEmail())
                .subject(subject)
                .text(text)
                .isHtml(false)
                .build();

        emailService.sendEmail(emailRequest);
        log.info("비밀번호 재설정 이메일 전송 완료: {}", userId);
    }

    /**
     * 비밀번호 재설정 토큰 검증
     * @param token 검증할 비밀번호 재설정 토큰
     * @throws com.akmz.springBase.auth.exception.InvalidResetTokenException 유효하지 않은 토큰일 경우 (404 Not Found)
     * @throws com.akmz.springBase.auth.exception.ExpiredResetTokenException 만료된 토큰일 경우 (410 Gone)
     */
    public void validatePasswordResetToken(String token) {
        AuthToken authToken = authTokenMapper.findByToken(token);

        // 1. 토큰 존재 여부 확인
        if (authToken == null) {
            throw new InvalidResetTokenException("유효하지 않은 비밀번호 재설정 링크입니다.");
        }

        // 2. 토큰 타입 및 사용 여부 확인
        if (!TOKEN_TYPE_PASSWORD_RESET.equals(authToken.getTokenType()) || authToken.isUsed()) {
            throw new InvalidResetTokenException("유효하지 않은 비밀번호 재설정 링크입니다.");
        }

        // 3. 토큰 만료 여부 확인
        if (authToken.getExpiryDate().before(new Date())) {
            throw new ExpiredResetTokenException("만료된 비밀번호 재설정 링크입니다.");
        }
    }

    /**
     * 비밀번호 재설정 처리
     * @param token 재설정 토큰
     * @param newPassword 새 비밀번호
     * @throws com.akmz.springBase.auth.exception.InvalidResetTokenException 유효하지 않은 토큰일 경우
     * @throws com.akmz.springBase.auth.exception.ExpiredResetTokenException 만료된 토큰일 경우
     * @throws UsernameNotFoundException 토큰에 해당하는 사용자가 없을 경우
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        AuthToken authToken = authTokenMapper.findByToken(token);

        // 1. 토큰 존재 여부 확인
        if (authToken == null) {
            throw new InvalidResetTokenException("유효하지 않은 비밀번호 재설정 링크입니다.");
        }

        // 2. 토큰 타입 및 사용 여부 확인
        if (!TOKEN_TYPE_PASSWORD_RESET.equals(authToken.getTokenType()) || authToken.isUsed()) {
            throw new InvalidResetTokenException("유효하지 않은 비밀번호 재설정 링크입니다.");
        }

        // 3. 토큰 만료 여부 확인
        if (authToken.getExpiryDate().before(new Date())) {
            throw new ExpiredResetTokenException("만료된 비밀번호 재설정 링크입니다.");
        }

        AuthUser user = authMapper.findByUsername(authToken.getUserName());
        if (user == null) {
            // 이 경우는 거의 발생하지 않아야 하지만, 방어적으로 코딩
            throw new UsernameNotFoundException("토큰에 해당하는 사용자를 찾을 수 없습니다.");
        }

        // 새 비밀번호 해싱 및 업데이트
        user.setPassword(passwordEncoder.encode(newPassword));
        authMapper.updateUserPassword(user); // AuthMapper에 이 메서드가 필요합니다.

        // 토큰 사용 처리
        authTokenMapper.updateAuthTokenUsed(token);

        log.info("비밀번호 재설정 완료: {}", user.getUserName());
    }

    /**
     * 유저 아이디로 이메일 주소 가져오기
     * @param userId
     * @return
     */
    public String getEmailAddr(String userId) {
        return authMapper.getEmailAddr(userId);
    }


}

