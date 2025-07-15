package com.akmz.springBase.login;

import com.akmz.springBase.base.mapper.AuthMapper;
import com.akmz.springBase.base.mapper.AuthTokenMapper;
import com.akmz.springBase.base.model.dto.EmailRequest;
import com.akmz.springBase.base.model.dto.LoginRequest;
import com.akmz.springBase.base.model.dto.PasswordResetConfirmRequest;
import com.akmz.springBase.base.model.dto.PasswordResetRequest;
import com.akmz.springBase.base.model.entity.AuthToken;
import com.akmz.springBase.base.model.entity.AuthUser;
import com.akmz.springBase.base.test.DotenvContextInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import com.akmz.springBase.base.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ContextConfiguration(initializers = DotenvContextInitializer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class PasswordResetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthMapper authMapper;

    @Autowired
    private AuthTokenMapper authTokenMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    @Test
    @DisplayName("비밀번호 재설정 요청 성공 테스트")
    @Sql(scripts = {"classpath:sql/clean_all_tables.sql", "classpath:data.sql", "classpath:sql/reset_user_login_status.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void password_reset_request_success_test() throws Exception {
        // given: 유효한 사용자 이메일
        PasswordResetRequest request = new PasswordResetRequest();
        request.setUserId("user"); // user는 data.sql에 있는 사용자 이름

        // EmailService.sendEmail 호출을 Mocking
        doNothing().when(emailService).sendEmail(any(EmailRequest.class));

        // when: 비밀번호 재설정 요청
        ResultActions result = mockMvc.perform(post("/api/auth/password/reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then: 200 OK 응답 확인 및 토큰 생성 확인
        result.andExpect(status().isOk())
                .andExpect(content().string("비밀번호 재설정 이메일이 전송되었습니다."));

        // DB에서 토큰이 생성되었는지 확인
        AuthToken latestToken = authTokenMapper.findLatestTokenByUserNameAndType(request.getUserId(), "PASSWORD_RESET");
        assertThat(latestToken).isNotNull();
        assertThat(latestToken.getToken()).isNotEmpty();
        assertThat(latestToken.getUserName()).isEqualTo(request.getUserId());
        assertThat(latestToken.getTokenType()).isEqualTo("PASSWORD_RESET");
        assertThat(latestToken.isUsed()).isFalse();
        assertThat(latestToken.getExpiryDate()).isAfter(new Date());
    }

    @Test
    @DisplayName("비밀번호 재설정 요청 실패 테스트 - 존재하지 않는 사용자")
    @Sql(scripts = {"classpath:sql/clean_all_tables.sql", "classpath:data.sql", "classpath:sql/reset_user_login_status.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void password_reset_request_fail_user_not_found_test() throws Exception {
        // given: 존재하지 않는 사용자 이메일
        PasswordResetRequest request = new PasswordResetRequest();
        request.setUserId("nonexistentUser");

        // EmailService.sendEmail 호출을 Mocking
        doNothing().when(emailService).sendEmail(any(EmailRequest.class));

        // when: 비밀번호 재설정 요청
        ResultActions result = mockMvc.perform(post("/api/auth/password/reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then: 404 Not Found 응답 확인
        result.andExpect(status().isNotFound())
                .andExpect(content().string("해당 이메일로 등록된 사용자가 없습니다: nonexistentUser"));
    }

    @Test
    @DisplayName("비밀번호 재설정 확인 성공 테스트")
    @Sql(scripts = "classpath:sql/reset_user_login_status.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void password_reset_confirm_success_test() throws Exception {
        // given: 유효한 재설정 토큰 생성
        String username = "user";
        String token = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 30 * 60 * 1000); // 30분 후 만료

        AuthToken authToken = new AuthToken();
        authToken.setToken(token);
        authToken.setUserName(username);
        authToken.setTokenType("PASSWORD_RESET");
        authToken.setExpiryDate(expiryDate);
        authToken.setCreatedDate(now);
        authToken.setUsed(false);
        authTokenMapper.insertAuthToken(authToken);

        String newPassword = "new_secure_password";
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken(token);
        request.setNewPassword(newPassword);

        // when: 비밀번호 재설정 확인 요청
        ResultActions result = mockMvc.perform(post("/api/auth/password/reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then: 200 OK 응답 확인
        result.andExpect(status().isOk())
                .andExpect(content().string("비밀번호가 성공적으로 재설정되었습니다."));

        // 비밀번호가 업데이트되었는지 확인
        AuthUser updatedUser = authMapper.findByUsername(username);
        assertThat(passwordEncoder.matches(newPassword, updatedUser.getPassword())).isTrue();

        // 토큰이 사용 처리되었는지 확인
        AuthToken usedToken = authTokenMapper.findByToken(token);
        assertThat(usedToken.isUsed()).isTrue();
    }

    @Test
    @DisplayName("비밀번호 재설정 확인 실패 테스트 - 유효하지 않은 토큰")
    void password_reset_confirm_fail_invalid_token_test() throws Exception {
        // given: 유효하지 않은 토큰
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken("invalid_token");
        request.setNewPassword("new_password");

        // when: 비밀번호 재설정 확인 요청
        ResultActions result = mockMvc.perform(post("/api/auth/password/reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then: 401 Unauthorized 응답 확인
        result.andExpect(status().isUnauthorized())
                .andExpect(content().string("유효하지 않거나 만료된 비밀번호 재설정 링크입니다."));
    }

    @Test
    @DisplayName("비밀번호 재설정 확인 실패 테스트 - 만료된 토큰")
    @Sql(scripts = "classpath:sql/reset_user_login_status.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void password_reset_confirm_fail_expired_token_test() throws Exception {
        // given: 만료된 재설정 토큰 생성
        String username = "user";
        String token = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() - 1000); // 1초 전 만료

        AuthToken authToken = new AuthToken();
        authToken.setToken(token);
        authToken.setUserName(username);
        authToken.setTokenType("PASSWORD_RESET");
        authToken.setExpiryDate(expiryDate);
        authToken.setCreatedDate(now);
        authToken.setUsed(false);
        authTokenMapper.insertAuthToken(authToken);

        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken(token);
        request.setNewPassword("new_password");

        // when: 비밀번호 재설정 확인 요청
        ResultActions result = mockMvc.perform(post("/api/auth/password/reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then: 401 Unauthorized 응답 확인
        result.andExpect(status().isUnauthorized())
                .andExpect(content().string("유효하지 않거나 만료된 비밀번호 재설정 링크입니다."));
    }

    @Test
    @DisplayName("비밀번호 재설정 확인 실패 테스트 - 이미 사용된 토큰")
    @Sql(scripts = "classpath:sql/reset_user_login_status.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void password_reset_confirm_fail_used_token_test() throws Exception {
        // given: 이미 사용된 재설정 토큰 생성
        String username = "user";
        String token = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 30 * 60 * 1000); // 30분 후 만료

        AuthToken authToken = new AuthToken();
        authToken.setToken(token);
        authToken.setUserName(username);
        authToken.setTokenType("PASSWORD_RESET");
        authToken.setExpiryDate(expiryDate);
        authToken.setCreatedDate(now);
        authToken.setUsed(true); // 이미 사용됨
        authTokenMapper.insertAuthToken(authToken);

        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken(token);
        request.setNewPassword("new_password");

        // when: 비밀번호 재설정 확인 요청
        ResultActions result = mockMvc.perform(post("/api/auth/password/reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then: 401 Unauthorized 응답 확인
        result.andExpect(status().isUnauthorized())
                .andExpect(content().string("유효하지 않거나 만료된 비밀번호 재설정 링크입니다."));
    }
}
