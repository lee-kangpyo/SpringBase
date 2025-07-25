package com.akmz.springBase.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 1️⃣ 모든 엔드포인트에 대해 CORS를 허용합니다.
                .allowedOrigins(
                        "http://localhost:3000",        // 로컬
                        "https://your-dev-frontend.com" // 예시
                ) // 2️⃣ 허용할 주소
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 3️⃣ 허용할 HTTP 메서드
                .allowedHeaders("*") // 4️⃣ 모든 헤더를 허용합니다. (인증 토큰 등)
                .allowCredentials(true) // 5️⃣ 인증 정보(쿠키, HTTP 인증, JWT 등)를 요청에 포함할 수 있도록 허용합니다.
                .maxAge(3600)           // 6️⃣ Preflight 요청 결과를 캐시할 시간 (초 단위)
                .exposedHeaders("content-disposition"); // ftp 다운로드 할때 res 헤더에 파일명 저장되는 위치
    }
}
