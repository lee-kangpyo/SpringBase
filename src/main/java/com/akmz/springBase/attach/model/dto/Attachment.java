package com.akmz.springBase.attach.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@Schema(description = "이메일 첨부파일 정보")
public class Attachment {

    @Schema(description = "이메일 클라이언트에 표시될 파일명", required = true, example = "document.pdf")
    private String name;

    @Schema(description = "파일의 MIME 타입 (예: application/pdf, image/jpeg)", required = true, example = "application/pdf")
    private String contentType;

    @Schema(description = "FTP 서버에 업로드된 파일의 경로 (URL). content 필드와 상호 배타적.", example = "/temp-uploads/2025/07/28/uuid_filename.pdf")
    private String url;

    @Schema(description = "파일의 바이트 배열 (서버에서 동적으로 생성된 파일용). url 필드와 상호 배타적.", format = "binary")
    private byte[] content;

}
