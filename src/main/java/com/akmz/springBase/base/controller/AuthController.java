package com.akmz.springBase.base.controller;

import com.akmz.springBase.base.exception.InvalidRefreshTokenException;
import com.akmz.springBase.base.exception.RefreshTokenMismatchException;
import com.akmz.springBase.base.model.dto.LoginRequest;
import com.akmz.springBase.base.model.dto.LogoutRequest;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "인증 관련 API")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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
        try {
            TokenResponse tokenResponse = authService.login(request);
            return ResponseEntity.ok(TokenResponse.builder()
                    .accessToken(tokenResponse.getAccessToken())
                    .refreshToken(tokenResponse.getRefreshToken())
                    .build());
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
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
            @RequestHeader(value = "X-Refresh-Token", required = true)
            @Parameter(description = "Refresh Token", required = true)
            String refreshToken
    ) {
        try {
            TokenResponse tokenResponse = authService.refreshAccessToken(refreshToken);
            return ResponseEntity.ok(tokenResponse);
        } catch (InvalidRefreshTokenException | JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 리프레시 토큰입니다.");
        } catch (RefreshTokenMismatchException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("리프레시 토큰이 일치하지 않습니다.(db 체크 오류)");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("알수없는 오류가 발생했습니다.");
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
        return ResponseEntity.ok("helloWord, akmz!");
    }
}