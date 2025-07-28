package com.akmz.springBase.attach.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@Schema(description = "임시 파일 업로드 응답 DTO")
public class TempFileResponse {

    @Schema(description = "FTP 서버에 저장된 임시 파일의 전체 경로 (URL)", example = "/temp-uploads/2025/07/28/uuid_filename.pdf")
    private String url;

    @Schema(description = "원본 파일명", example = "document.pdf")
    private String name;

    @Schema(description = "파일의 MIME 타입 (예: application/pdf, image/jpeg)", example = "application/pdf")
    private String contentType;
}