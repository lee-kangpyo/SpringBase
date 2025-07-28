package com.akmz.springBase.notification.email.controller;

import com.akmz.springBase.notification.email.model.dto.EmailRequest;
import com.akmz.springBase.notification.email.service.EmailService;
import jakarta.mail.MessagingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Tag(name = "이메일 API", description = "이메일 전송 관련 API")
@Slf4j
public class EmailController {

    private final EmailService emailService;

    @Operation(summary = "이메일 전송", description = "지정된 수신자에게 이메일을 전송합니다. 첨부파일 URL 또는 byte[]를 포함할 수 있습니다.")
    @ApiResponse(responseCode = "200", description = "이메일 전송 성공")
    @ApiResponse(responseCode = "500", description = "이메일 전송 실패")
    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@RequestBody(description = "이메일 전송 요청 본문", required = true) @org.springframework.web.bind.annotation.RequestBody EmailRequest emailRequest) {
        try {
            emailService.sendEmail(emailRequest);
            return ResponseEntity.ok("이메일 전송 성공");
        } catch (MessagingException e) {
            log.error("이메일 전송 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("이메일 전송 실패: " + e.getMessage());
        }
    }
}
