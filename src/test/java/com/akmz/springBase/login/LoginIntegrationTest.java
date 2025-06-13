package com.akmz.springBase.login;

import com.akmz.springBase.base.model.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
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
}
