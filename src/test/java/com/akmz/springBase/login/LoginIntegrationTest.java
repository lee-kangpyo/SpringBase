package com.akmz.springBase.login;

import com.akmz.springBase.base.model.dto.LoginRequest;
import com.akmz.springBase.base.test.DotenvContextInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ContextConfiguration(initializers = DotenvContextInitializer.class)
class LoginIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("로그인 성공 테스트 - JWT 토큰 반환")
    void login_success_test() throws Exception {
        // given: 요청 객체 생성
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUserName("user");
        loginRequest.setPassword("user");

        // when: 새로운 API 엔드포인트로 POST 요청 수행
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))); // 요청 본문을 JSON 문자열로 설정

        // then: 성공적인 API 로그인에 대한 예상 결과 검증
        result.andExpect(status().isOk()) // HTTP 200 OK를 기대함
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").isNotEmpty()) // accessToken이 존재하고 비어 있지 않은지 확인
                .andExpect(jsonPath("$.refreshToken").isNotEmpty()); // refreshToken이 존재하고 비어 있지 않은지 확인
    }

    @Test
    @DisplayName("로그인 실패 테스트 - 401 Unauthorized 반환")
    void login_fail_test() throws Exception {
        // given: 유효하지 않은 인증 정보로 요청 객체 생성
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUserName("user");
        loginRequest.setPassword("wrong_password");

        // when: POST 요청 수행
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)));

        // then: API 로그인 실패 시 예상 결과 검증
        result.andExpect(status().isUnauthorized()) // HTTP 401 Unauthorized 상태를 기대함
                .andExpect(content().string("Invalid credentials"));
    }

    @Test
    @DisplayName("로그아웃 성공 테스트 - 유효한 JWT 토큰으로 로그아웃")
    void logout_success_test() throws Exception {
        // given: 로그인 성공 후 Access Token 확보
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUserName("user");
        loginRequest.setPassword("user");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // 로그인 응답에서 Access Token 추출
        String responseBody = loginResult.getResponse().getContentAsString();
        // Jackson ObjectMapper를 사용하여 JSON 응답 파싱
        String accessToken = objectMapper.readTree(responseBody).get("accessToken").asText();

        // when: 확보한 Access Token으로 로그아웃 요청 수행
        ResultActions logoutResult = mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + accessToken) // Authorization 헤더에 Access Token 포함
                .contentType(MediaType.APPLICATION_JSON)); // 요청 본문은 없어도 됨

        // then: 로그아웃 성공 검증 (HTTP 200 OK)
        logoutResult.andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그아웃 실패 테스트 - JWT 토큰 없이 로그아웃 시도 (401 Unauthorized)")
    void logout_fail_no_token_test() throws Exception {
        // when: JWT 토큰 없이 로그아웃 요청 수행
        ResultActions logoutResult = mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)); // 요청 본문은 없어도 됨

        // then: 인증 정보가 없으므로 401 Unauthorized 기대
        logoutResult.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰 재발급 성공 테스트 - 유효한 Refresh Token으로 Access Token 재발급")
    void refresh_token_reissue_success_test() throws Exception {
        // given: 로그인 성공 후 Refresh Token 확보
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUserName("user");
        loginRequest.setPassword("user");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // 로그인 응답에서 Refresh Token 추출
        String responseBody = loginResult.getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(responseBody).get("refreshToken").asText();

        // when: 확보한 Refresh Token으로 재발급 요청 수행
        ResultActions reissueResult = mockMvc.perform(post("/api/auth/token/reissue")
                .header("X-Refresh-Token", refreshToken) // X-Refresh-Token 헤더에 Refresh Token 포함
                .contentType(MediaType.APPLICATION_JSON));

        // then: 재발급 성공 검증
        reissueResult.andExpect(status().isOk()) // HTTP 200 OK를 기대함
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").isNotEmpty()) // 새로운 accessToken이 존재하고 비어 있지 않은지 확인
                .andExpect(jsonPath("$.refreshToken").isNotEmpty()); // 새로운 refreshToken이 존재하고 비어 있지 않은지 확인
    }

    @Test
    @DisplayName("토큰 재발급 실패 테스트 - 유효하지 않은 Refresh Token (401 Unauthorized)")
    void refresh_token_reissue_fail_invalid_token_test() throws Exception {
        // given: 유효하지 않은 Refresh Token
        String invalidRefreshToken = "invalid.refresh.token.example";

        // when: 유효하지 않은 Refresh Token으로 재발급 요청 수행
        ResultActions reissueResult = mockMvc.perform(post("/api/auth/token/reissue")
                .header("X-Refresh-Token", invalidRefreshToken) // X-Refresh-Token 헤더에 유효하지 않은 토큰 포함
                .contentType(MediaType.APPLICATION_JSON));

        // then: 401 Unauthorized 상태와 예상 메시지 검증
        reissueResult.andExpect(status().isUnauthorized()) // HTTP 401 Unauthorized를 기대함
                .andExpect(content().string("유효하지 않은 리프레시 토큰입니다.")); // 컨트롤러의 응답 메시지 확인
    }
}
