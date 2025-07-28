package com.akmz.springBase.admin;

import com.akmz.springBase.auth.mapper.AuthMapper;
import com.akmz.springBase.auth.model.dto.LoginRequest;
import com.akmz.springBase.common.test.DotenvContextInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ContextConfiguration(initializers = DotenvContextInitializer.class)
@Sql(scripts = {"classpath:sql/clean_all_tables.sql", "classpath:schema.sql", "classpath:data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class AdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthMapper authMapper; // 사용자 권한 확인 등을 위해 필요할 수 있음

    // Helper method to get an access token for a given user
    private String getAccessToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUserName(username);
        loginRequest.setPassword(password);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    @DisplayName("관리자 사용자 목록 조회 성공 테스트 - ADMIN 권한")
    
    void admin_user_list_success_test() throws Exception {
        // given: ADMIN 권한을 가진 사용자 로그인
        String adminAccessToken = getAccessToken("admin", "admin"); // data.sql에 admin/admin 사용자 존재

        // when: 관리자 사용자 목록 조회 API 호출
        ResultActions result = mockMvc.perform(get("/api/admin/userList")
                .header("Authorization", "Bearer " + adminAccessToken)
                .contentType(MediaType.APPLICATION_JSON));

        // then: 200 OK 응답 및 사용자 목록 반환 확인
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].userName").exists()); // 최소한 하나의 사용자 존재 확인
    }

    @Test
    @DisplayName("관리자 사용자 목록 조회 실패 테스트 - USER 권한 (403 Forbidden)")
    
    void admin_user_list_forbidden_test() throws Exception {
        // given: USER 권한을 가진 사용자 로그인
        String userAccessToken = getAccessToken("user", "user"); // data.sql에 user/user 사용자 존재

        // when: 관리자 사용자 목록 조회 API 호출
        ResultActions result = mockMvc.perform(get("/api/admin/userList")
                .header("Authorization", "Bearer " + userAccessToken)
                .contentType(MediaType.APPLICATION_JSON));

        // then: 403 Forbidden 응답 확인
        result.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자 사용자 목록 조회 실패 테스트 - 인증되지 않은 사용자 (401 Unauthorized)")
    
    void admin_user_list_unauthorized_test() throws Exception {
        // when: 인증되지 않은 상태로 관리자 사용자 목록 조회 API 호출
        ResultActions result = mockMvc.perform(get("/api/admin/userList")
                .contentType(MediaType.APPLICATION_JSON));

        // then: 401 Unauthorized 응답 확인
        result.andExpect(status().isUnauthorized());
    }
}
