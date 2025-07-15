package com.akmz.springBase.login;

import com.akmz.springBase.base.model.dto.LoginRequest;
import com.akmz.springBase.base.model.entity.AuthUser;
import com.akmz.springBase.base.test.DotenvContextInitializer;
import com.akmz.springBase.base.mapper.AuthMapper;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.mybatis.spring.SqlSessionTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ContextConfiguration(initializers = DotenvContextInitializer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LoginIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(LoginIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthMapper authMapper;

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    @Test
    @Transactional
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
                .andExpect(jsonPath("$.accessToken").isNotEmpty()); // accessToken이 존재하고 비어 있지 않은지 확인
    }

    @Test
    @Transactional
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
                .andExpect(content().string("아이디 또는 비밀번호가 일치하지 않습니다."));
    }

    

    @Test
    @Transactional
    @DisplayName("로그아웃 성공 테스트 - 유효한 JWT 토큰으로 로그아웃, db에서 refresh 토큰 제거 체크")
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

        // then: DB에서 Refresh Token이 null로 업데이트되었는지 확인
        AuthUser userAfterLogout = authMapper.findByUsername(loginRequest.getUserName());
        assertThat(userAfterLogout.getRefreshToken()).isNull();
    }

    @Test
    @Transactional
    @DisplayName("로그아웃 실패 테스트 - JWT 토큰 없이 로그아웃 시도 (401 Unauthorized)")
    void logout_fail_no_token_test() throws Exception {
        // when: JWT 토큰 없이 로그아웃 요청 수행
        ResultActions logoutResult = mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)); // 요청 본문은 없어도 됨

        // then: 인증 정보가 없으므로 401 Unauthorized 기대
        logoutResult.andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    @DisplayName("토큰 재발급 성공 테스트 - 유효한 Refresh Token으로 Access Token 재발급, db 업데이트 확인")
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

        // 로그인 응답에서 Refresh Token 추출 (Set-Cookie 헤더에서)
        String setCookieHeader = loginResult.getResponse().getHeader("Set-Cookie");
        String refreshToken = null;
        if (setCookieHeader != null) {
            // "refresh_token=" 다음부터 첫 번째 세미콜론(;) 전까지의 문자열 추출
            int startIndex = setCookieHeader.indexOf("refresh_token=");
            if (startIndex != -1) {
                startIndex += "refresh_token=".length();
                int endIndex = setCookieHeader.indexOf(";", startIndex);
                if (endIndex != -1) {
                    refreshToken = setCookieHeader.substring(startIndex, endIndex);
                } else {
                    // 세미콜론이 없는 경우 (마지막 쿠키일 경우)
                    refreshToken = setCookieHeader.substring(startIndex);
                }
            }
        }

        // when: 확보한 Refresh Token으로 재발급 요청 수행
        ResultActions reissueResult = mockMvc.perform(post("/api/auth/token/reissue")
                .header("X-Refresh-Token", refreshToken) // X-Refresh-Token 헤더에 Refresh Token 포함
                .contentType(MediaType.APPLICATION_JSON));

        // then: 재발급 성공 검증
        reissueResult.andExpect(status().isOk()) // HTTP 200 OK를 기대함
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").isNotEmpty()) // 새로운 accessToken이 존재하고 비어 있지 않은지 확인
                .andExpect(jsonPath("$.refreshToken").isNotEmpty()); // 새로운 refreshToken이 존재하고 비어 있지 않은지 확인

        // then: DB에 저장된 Refresh Token이 새로운 토큰으로 업데이트되었는지 확인
        sqlSessionTemplate.clearCache(); // MyBatis 캐시 클리어
        AuthUser userAfterReissue = authMapper.findByUsername(loginRequest.getUserName());
        assertThat(userAfterReissue.getRefreshToken()).isEqualTo(objectMapper.readTree(reissueResult.andReturn().getResponse().getContentAsString()).get("refreshToken").asText());
        assertThat(userAfterReissue.getRefreshToken()).isNotEqualTo(refreshToken); //  db에 저장된 refreshToken과 이전 토큰은 달라야함
    }

    @Test
    @Transactional
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

    @Test
    @Transactional
    @DisplayName("비활성화된 계정 로그인 테스트 - 401 Unauthorized 반환")
    void login_disabled_account_test() throws Exception {
        // given: 비활성화된 계정 정보
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUserName("disabledUser");
        loginRequest.setPassword("user"); // 비밀번호는 올바르지만 계정이 비활성화됨

        // when: 로그인 시도
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)));

        // then: 401 Unauthorized 상태와 예상 메시지 검증
        result.andExpect(status().isUnauthorized())
                .andExpect(content().string("계정이 비활성화되었습니다. 관리자에게 문의하세요."));
    }

    @Test
    @Transactional
    @DisplayName("로그아웃 실패 테스트 - 유효하지 않은 Access Token으로 로그아웃 시도 (401 Unauthorized)")
    void logout_fail_invalid_access_token_test() throws Exception {
        // given: 유효하지 않은 Access Token
        String invalidAccessToken = "Bearer invalid.access.token.example";

        // when: 유효하지 않은 Access Token으로 로그아웃 요청 수행
        ResultActions logoutResult = mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", invalidAccessToken) // Authorization 헤더에 유효하지 않은 토큰 포함
                .contentType(MediaType.APPLICATION_JSON));

        // then: 401 Unauthorized 상태를 기대함
        logoutResult.andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    @DisplayName("로그아웃 실패 테스트 - 만료된 Access Token으로 로그아웃 시도 (401 Unauthorized)")
    void logout_fail_expired_access_token_test() throws Exception {
        // given: 만료된 Access Token (실제 만료된 토큰을 생성하기 어려우므로 임의의 만료된 토큰 문자열 사용)
        String expiredAccessToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzkwMjJ9.invalid_signature_for_expired_token";

        // when: 만료된 Access Token으로 로그아웃 요청 수행
        ResultActions logoutResult = mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", expiredAccessToken) // Authorization 헤더에 만료된 토큰 포함
                .contentType(MediaType.APPLICATION_JSON));

        // then: 401 Unauthorized 상태를 기대함
        logoutResult.andExpect(status().isUnauthorized());
    }

    

    @Test
    @Transactional
    @DisplayName("보호된 API 접근 테스트 - 토큰 없이 접근 시 401 Unauthorized 반환")
    void protected_api_access_no_token_test() throws Exception {
        // given: 보호된 API 엔드포인트
        String pathVariable = "testPath";
        String queryString = "testQuery";

        // when: 토큰 없이 보호된 API에 GET 요청 수행
        ResultActions result = mockMvc.perform(get("/api/auth/helloWord/{path}", pathVariable)
                .param("queryString", queryString)
                .contentType(MediaType.APPLICATION_JSON));

        // then: 401 Unauthorized 상태를 기대함
        result.andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    @DisplayName("보호된 API 접근 테스트 - 유효하지 않은 형식의 토큰으로 접근 시 401 Unauthorized 반환")
    void protected_api_access_invalid_token_format_test() throws Exception {
        // given: 유효하지 않은 형식의 토큰
        String invalidToken = "Bearer invalid.token.format";
        String pathVariable = "testPath";
        String queryString = "testQuery";

        // when: 유효하지 않은 토큰으로 보호된 API에 GET 요청 수행
        ResultActions result = mockMvc.perform(get("/api/auth/helloWord/{path}", pathVariable)
                .param("queryString", queryString)
                .header("Authorization", invalidToken)
                .contentType(MediaType.APPLICATION_JSON));

        // then: 401 Unauthorized 상태를 기대함
        result.andExpect(status().isUnauthorized());
    }

    
    @Test
    @Transactional
    @DisplayName("보호된 API 접근 테스트 - 만료된 토큰으로 접근 시 401 Unauthorized 반환 (유효하지 않은 토큰으로 대체)")
    void protected_api_access_expired_token_test() throws Exception {
        // given: 만료된 토큰 (여기서는 유효하지 않은 임의의 토큰으로 대체)
        // 실제 만료된 토큰을 생성하려면 JwtTokenProvider를 Mocking하거나,
        // 테스트 환경에서 토큰 만료 시간을 매우 짧게 설정하는 추가적인 설정이 필요합니다.
        String expiredToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzkwMjJ9.invalid_signature_for_expired_token";
        String pathVariable = "testPath";
        String queryString = "testQuery";

        // when: 만료된 토큰으로 보호된 API에 GET 요청 수행
        ResultActions result = mockMvc.perform(get("/api/auth/helloWord/{path}", pathVariable)
                .param("queryString", queryString)
                .header("Authorization", expiredToken)
                .contentType(MediaType.APPLICATION_JSON));

        // then: 401 Unauthorized 상태를 기대함
        result.andExpect(status().isUnauthorized());
    }
}
