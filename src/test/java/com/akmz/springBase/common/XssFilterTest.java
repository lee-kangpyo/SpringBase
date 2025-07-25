package com.akmz.springBase.common;

import com.akmz.springBase.auth.model.dto.LoginRequest;
import com.akmz.springBase.common.test.DotenvContextInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ContextConfiguration(initializers = DotenvContextInitializer.class)

class XssFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
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
        accessToken = objectMapper.readTree(responseBody).get("accessToken").asText();
    }

    @Test
    @DisplayName("XSS 필터 테스트 - 쿼리 문자열에 대한 XSS 필터링 검증")
    void xss_filter_test() throws Exception {
        // given: XSS 공격 페이로드
        String pathVariable = "testPath";
        String xssPayload = "<script>alert('XSS');</script>";
        // XssRequestWrapper에서 script 태그는 제거되고, 특수문자는 HTML 엔티티로 변환됨
        String expectedFilteredOutput = "helloWord, testPath! queryString=";

        // when: XSS 페이로드를 포함하여 GET 요청 수행
        mockMvc.perform(get("/api/auth/helloWord/{path}", pathVariable)
                        .param("queryString", xssPayload)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedFilteredOutput));
    }
}
