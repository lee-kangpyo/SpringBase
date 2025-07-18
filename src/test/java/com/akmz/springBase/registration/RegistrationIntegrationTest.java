package com.akmz.springBase.registration;

import com.akmz.springBase.SpringBaseApplication;
import com.akmz.springBase.base.mapper.AuthMapper;
import com.akmz.springBase.base.model.dto.UserRegistrationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {"JWT_SECRET_KEY=test_secret_key_for_testing"})
@Transactional
@Sql(scripts = {"classpath:/sql/clean_all_tables.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class RegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthMapper authMapper;

    private UserRegistrationRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new UserRegistrationRequest();
        validRequest.setUserName("testuser");
        validRequest.setEmail("test@example.com");
        validRequest.setPassword("Password123!");
    }

    @Test
    @DisplayName("성공적인 회원가입")
    void testSuccessfulRegistration() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("회원가입이 성공적으로 완료되었습니다."));

        // DB에 사용자 정보가 올바르게 저장되었는지 확인
        assertThat(authMapper.findByUsername("testuser")).isNotNull();
        assertThat(authMapper.existsByEmail("test@example.com")).isTrue();
    }

    @Test
    @DisplayName("유효하지 않은 이메일 형식으로 회원가입 시도 시 400 에러 반환")
    void testRegistrationWithInvalidEmail() throws Exception {
        validRequest.setEmail("invalid-email");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("올바른 이메일 형식을 입력해주세요."));
    }

    @Test
    @DisplayName("비밀번호가 8자 미만일 경우 400 에러 반환")
    void testRegistrationWithShortPassword() throws Exception {
        validRequest.setPassword("Aa1");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").value("비밀번호는 8자 이상이어야 합니다."));
    }

    @Test
    @DisplayName("비밀번호에 영문 대소문자 및 숫자가 포함되지 않을 경우 400 에러 반환")
    void testRegistrationWithWeakPassword() throws Exception {
        validRequest.setPassword("password"); // 숫자 없음

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").value("비밀번호는 영문 대소문자와 숫자를 포함해야 합니다."));
    }

    @Test
    @DisplayName("중복된 이메일로 회원가입 시도 시 409 에러 반환")
    void testRegistrationWithDuplicateEmail() throws Exception {
        // 첫 번째 사용자 등록 (성공)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        // 동일한 이메일로 두 번째 사용자 등록 시도 (실패 예상)
        UserRegistrationRequest duplicateEmailRequest = new UserRegistrationRequest();
        duplicateEmailRequest.setUserName("anotheruser");
        duplicateEmailRequest.setEmail("test@example.com"); // 중복 이메일
        duplicateEmailRequest.setPassword("NewPassword123!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateEmailRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$").value("이미 등록된 이메일입니다."));
    }

    @Test
    @DisplayName("중복된 사용자명으로 회원가입 시도 시 409 에러 반환")
    void testRegistrationWithDuplicateUsername() throws Exception {
        // 첫 번째 사용자 등록 (성공)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        // 동일한 사용자명으로 두 번째 사용자 등록 시도 (실패 예상)
        UserRegistrationRequest duplicateUsernameRequest = new UserRegistrationRequest();
        duplicateUsernameRequest.setUserName("testuser"); // 중복 사용자명
        duplicateUsernameRequest.setEmail("another@example.com");
        duplicateUsernameRequest.setPassword("NewPassword123!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUsernameRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$").value("이미 사용 중인 사용자명입니다."));
    }
}
