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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ContextConfiguration(initializers = DotenvContextInitializer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AccountLockIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("계정 잠금 테스트 - 5회 로그인 실패 시 계정 잠금")
    @Sql(scripts = "classpath:sql/reset_user_login_status.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void account_lock_test() throws Exception {
        // given: 5회 연속 로그인 실패
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUserName("user");
        loginRequest.setPassword("wrong_password");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)));
        }

        // when: 6번째 로그인 시도
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)));

        // then: 계정 잠금 확인
        result.andExpect(status().isUnauthorized())
                .andExpect(content().string("User account is locked"));
    }
}
