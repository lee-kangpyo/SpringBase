package com.akmz.springBase.base.controller;

import com.akmz.springBase.base.exception.ExpiredResetTokenException;
import com.akmz.springBase.base.exception.InvalidRefreshTokenException;
import com.akmz.springBase.base.exception.InvalidResetTokenException;
import com.akmz.springBase.base.exception.RefreshTokenMismatchException;
import com.akmz.springBase.base.model.dto.LoginRequest;
import com.akmz.springBase.base.model.dto.PasswordResetConfirmRequest;
import com.akmz.springBase.base.model.dto.PasswordResetRequest;
import com.akmz.springBase.base.model.dto.TokenResponse;
import com.akmz.springBase.base.service.AuthService;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import com.akmz.springBase.base.model.dto.UserRegistrationRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "인증 관련 API")
@RequiredArgsConstructor
@Log4j2
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.refresh-expiration}")
    private int refreshExpiration;

    @Value("${spring.profiles.active}")
    private String MODE;

    @PostMapping("/login")
    @Operation(
            summary = "로그인 API",
            description = "사용자 인증 정보를 받아서 인증 후 JWT Access Token과 Refresh Token을 생성하여 반환한다. 인증이 필요하지 않음",
            responses = {
                @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(schema = @Schema(implementation = TokenResponse.class))),
                @ApiResponse(responseCode = "400", description = "잘못된 요청 - 입력값 검증 실패"),
                @ApiResponse(responseCode = "401", description = "인증 실패")
            }
    )
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request) {
        log.info("login");
        try {
            try {
                TokenResponse tokenResponse = authService.login(request);
                ResponseCookie token = ResponseCookie.from("X-Refresh-Token", tokenResponse.getRefreshToken())
                        .httpOnly(true)
                        .secure("PROD".equals(MODE))
                        .maxAge(refreshExpiration / 1000)
                        .sameSite("Lax")
                        .path("/")
                        .build();
                // 쿠키에 포함한 리프레쉬 토큰 제거
            tokenResponse.setRefreshToken(null);

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, token.toString())
                        .body(tokenResponse);
            } catch (BadCredentialsException e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("아이디 또는 비밀번호가 일치하지 않습니다.");
            } catch (LockedException e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage()); // 계정 잠김 메시지 그대로 전달
//            } catch (UsernameNotFoundException e) { 해당 exception은 사용자 열거 공격(User Enumeration Attack)을 막기 위해 사용하지 않고 BadCredentialsException으로 합쳐서 리턴
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("존재하지 않는 사용자입니다.");
            } catch (DisabledException e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("계정이 비활성화되었습니다. 관리자에게 문의하세요.");
            } catch (AuthenticationException e) { // 그 외 Spring Security 인증 관련 예외
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증 실패: " + e.getMessage());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
            }
//            TokenResponse tokenResponse = authService.login(request);
//            return ResponseEntity.ok(TokenResponse.builder()
//                    .accessToken(tokenResponse.getAccessToken())
//                    .refreshToken(tokenResponse.getRefreshToken())
//                    .build());
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    @PostMapping("/register")
    @Operation(
            summary = "회원가입 API",
            description = "사용자 정보를 받아서 회원가입을 처리한다.",
            responses = {
                @ApiResponse(responseCode = "200", description = "회원가입 성공", content = @Content(schema = @Schema(implementation = String.class))),
                @ApiResponse(responseCode = "400", description = "잘못된 요청 - 입력값 검증 실패"),
                @ApiResponse(responseCode = "409", description = "회원가입 실패 - 이미 존재하는 사용자")
            }
    )
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        log.info("registerUser request: {}", request.getEmail());
        try {
            authService.registerUser(request);
            return ResponseEntity.ok("회원가입이 성공적으로 완료되었습니다.");
        } catch (com.akmz.springBase.base.exception.UserAlreadyExistsException e) { // Full qualified name for now
            log.warn("User registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error during user registration: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("회원가입 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/logout")
    @Operation(
            summary = "로그아웃 API",
            description = "db에 저장되어있는 Refresh Token을 삭제한다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "로그아웃 성공", content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            }
    )
    public ResponseEntity<?> logout(Authentication authentication) {
        String userId = authentication.getName();
        authService.logout(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/token/reissue")
    @Operation(
            summary = "토큰 재발급",
            description = "represh token 으로 access token을 재발급한다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "발급 성공", content = @Content(schema = @Schema(implementation = TokenResponse.class))),
                    @ApiResponse(responseCode = "401", description = "유효하지 않는 리프레쉬 토큰")
            }
    )
    public ResponseEntity<?> refreshAccessToken(
            @CookieValue(value = "X-Refresh-Token", required = true)
            String refreshToken
    ) {
        try {
            TokenResponse tokenResponse = authService.refreshAccessToken(refreshToken);
            ResponseCookie token = ResponseCookie.from("X-Refresh-Token", tokenResponse.getRefreshToken())
                    .httpOnly(true)
                    .secure("PROD".equals(MODE))
                    .maxAge(refreshExpiration / 1000)
                    .sameSite("Lax")
                    .path("/")
                    .build();
            // 쿠키에 포함한 리프레쉬 토큰 제거
            tokenResponse.setRefreshToken(null);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, token.toString())
                    .body(tokenResponse);
        } catch (InvalidRefreshTokenException | JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 리프레시 토큰입니다.");
        } catch (RefreshTokenMismatchException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("리프레시 토큰이 일치하지 않습니다.(db 체크 오류)");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("알수없는 오류가 발생했습니다.");
        }
    }

    @PostMapping("/password/reset/request")
    @Operation(
            summary = "비밀번호 재설정 요청",
            description = "사용자 이메일로 비밀번호 재설정 링크를 전송한다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "비밀번호 재설정 이메일 전송 성공"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청 - 이메일 전송 실패"),
                    @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
                    @ApiResponse(responseCode = "500", description = "이메일 전송 실패 또는 서버 오류")
            }
    )
    public ResponseEntity<?> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        try {
//            String email = authService.getEmailAddr(request.getUserId());
            authService.requestPasswordReset(request.getUserId());
            return ResponseEntity.ok("비밀번호 재설정 이메일이 전송되었습니다.");
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (MessagingException e) {
            log.error("비밀번호 재설정 이메일 전송 실패: {}", e.getMessage());
            String errorMessage = "비밀번호 재설정 이메일 전송에 실패했습니다.";
            // 'Invalid Addresses' 메시지가 포함되어 있다면 더 구체적인 메시지 제공
            if (e.getMessage() != null && e.getMessage().contains("Invalid Addresses")) {
                errorMessage = "입력하신 이메일 주소가 유효하지 않거나 존재하지 않습니다. 다시 확인해주세요.";
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
        } catch (Exception e) {
            log.error("비밀번호 재설정 요청 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
        }
    }

    @GetMapping("/validate-reset-token")
    @Operation(
            summary = "비밀번호 재설정 토큰 유효성 검증",
            description = "비밀번호 재설정 페이지에 진입했을 때, URL의 토큰이 유효한지 사전에 검증한다.",
            parameters = {
                    @Parameter(name = "token", description = "이메일로 발송된 비밀번호 재설정 토큰", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "토큰 유효성 검증 성공 (유효한 토큰)"),
                    @ApiResponse(responseCode = "404", description = "유효하지 않은 토큰 (존재하지 않거나, 타입이 다르거나, 이미 사용된 토큰)", content = @Content),
                    @ApiResponse(responseCode = "410", description = "만료된 토큰", content = @Content)
            }
    )
    public ResponseEntity<?> validateResetToken(@RequestParam String token) {
        try {
            authService.validatePasswordResetToken(token);
            return ResponseEntity.ok().build();
        } catch (InvalidResetTokenException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ExpiredResetTokenException e) {
            return ResponseEntity.status(HttpStatus.GONE).body(e.getMessage());
        }
    }

    @PostMapping("/password/reset/confirm")
    @Operation(
            summary = "비밀번호 재설정 확인",
            description = "재설정 토큰과 새 비밀번호를 받아 비밀번호를 업데이트한다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "비밀번호 재설정 성공"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청 - 입력값 검증 실패"),
                    @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 토큰"),
                    @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            }
    )
    public ResponseEntity<?> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        try {
            authService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok("비밀번호가 성공적으로 재설정되었습니다.");
        } catch (InvalidResetTokenException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ExpiredResetTokenException e) {
            return ResponseEntity.status(HttpStatus.GONE).body(e.getMessage());
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("비밀번호 재설정 확인 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
        }
    }

    @GetMapping("/helloWord/{path}")
    @Operation(
            summary = "테스트용 인증 필요 API",
            description = "JWT 인증이 필요한 API로, Swagger 문서에 자물쇠(보안 요구사항) 표시가 된다."
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<String> hello(
            @Parameter(description = "경로변수", required = true)
            @PathVariable String path,
            @Parameter(description = "쿼리스트링", required = true)
            @RequestParam String queryString
    ) {
        return ResponseEntity.ok("helloWord, " + path + "! queryString=" + queryString);
    }
}
