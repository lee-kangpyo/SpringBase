package com.akmz.springBase.notification.email.model.dto;

import com.akmz.springBase.attach.model.dto.Attachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@Schema(description = "이메일 전송 요청 DTO")
public class EmailRequest {

    @Schema(description = "수신자 이메일 주소 목록", required = true, example = "[\"recipient1@example.com\", \"recipient2@example.com\"]")
    private List<String> to;

    @Schema(description = "이메일 제목", required = true, example = "테스트 이메일입니다.")
    private String subject;

    @Schema(description = "이메일 본문 (HTML 또는 일반 텍스트)", required = true, example = "<h1>안녕하세요,</h1><p>이것은 테스트 이메일입니다.</p>")
    private String text;

    @Schema(description = "참조 수신자 이메일 주소 목록", example = "[\"cc_recipient@example.com\"]")
    private List<String> cc;

    @Schema(description = "숨은 참조 수신자 이메일 주소 목록", example = "[\"bcc_recipient@example.com\"]")
    private List<String> bcc;

    @Schema(description = "본문이 HTML 형식인지 여부 (기본값: false)", defaultValue = "false")
    private boolean isHtml;

    @Schema(description = "첨부파일 목록. url 또는 content 중 하나를 포함해야 합니다.")
    private List<Attachment> attachments;
}
