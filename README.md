# Spring Boot Base Project

이 프로젝트는 JWT 인증, 역할 기반 접근 제어(RBAC), 동적 메뉴 관리 등 최신 웹 애플리케이션에 필요한 핵심 기능들을 포함하는 스프링 부트 기반의 백엔드 템플릿 프로젝트입니다.

## ✨ 주요 기능

-   **인증 (Authentication)**: JWT(JSON Web Token)를 사용한 상태 비저장 인증 시스템
    -   로그인, 회원가입, 토큰 재발급 API
    -   소셜 로그인 (Google, Naver) 연동 기반
-   **인가 (Authorization)**: Spring Security를 활용한 역할 기반 접근 제어 (RBAC)
    -   URL 및 메소드 레벨에서 역할에 따른 접근 권한 관리
-   **동적 메뉴 관리**: 사용자의 역할에 따라 접근 가능한 메뉴를 동적으로 생성하여 제공
-   **관리자 기능**: 사용자, 역할(Role), 리소스(메뉴, API)를 관리하는 API 제공
-   **파일 업로드**: FTP를 이용한 파일 업로드 기능
-   **보안**: XSS(Cross-Site Scripting) 방지 필터 적용
-   **API 문서화**: Swagger (OpenAPI 3)를 이용한 자동 API 문서화

## 🛠️ 기술 스택

-   **Backend**: Java 17, Spring Boot 3.4.6, Spring Security
-   **Database**: H2 (개발용), MyBatis 3
-   **Build Tool**: Gradle
-   **API Docs**: SpringDoc (Swagger UI)
-   **Authentication**: JSON Web Token (JWT)
-   **etc**: Lombok, Log4j2, PageHelper

## 🚀 시작하기

### 1. 사전 요구사항

-   Java 17
-   Gradle 8.x

### 2. 프로젝트 실행

```bash
# 1. 프로젝트 복제
git clone [저장소 URL]
cd [프로젝트 폴더]

# 2. 환경변수 설정
JWT_SECRET_KEY=<JWT_SECRET_KEY>
EMAIL_ID=<EMAIL_ID>
EMAIL_PW=<EMAIL_PW>

# FTP
FTP_HOST=<FTP_HOST>
FTP_PORT=<FTP_PORT>
FTP_USERNAME=<FTP_USERNAME>
FTP_PASSWORD=<FTP_PASSWORD>
FTP_REMOTE_BASE_DIR=<FTP_REMOTE_BASE_DIR>

# 임시 파일 저장 경로(현재는 이메일 첨부에 사용됨)
FTP_TEMP_UPLOADS_DIR=<FTP_TEMP_UPLOADS_DIR>
FTP_TEMP_FILE_EXPIRATION_HOURS=1 # 만료일 1시간 마다 clean up


# 3. 프로젝트 빌드
./gradlew build

# 4. 애플리케이션 실행
./gradlew bootRun
```

### 3. API 문서 확인

애플리케이션 실행 후, 아래 주소에서 API 문서를 확인할 수 있습니다.
-   [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## 📝 API 엔드포인트 개요

-   `/api/auth/**`: 로그인, 회원가입 등 사용자 인증 관련 API
-   `/api/menu/**`: 현재 로그인한 사용자의 역할에 맞는 메뉴 조회 API
-   `/api/admin/**`: 사용자, 역할, 리소스 관리 등 관리자 기능 API
-   `/api/attach/**`: 파일 업로드, 다운로드 등 첨부파일 관리 API
-   `/api/email/**`: 이메일 발송 API