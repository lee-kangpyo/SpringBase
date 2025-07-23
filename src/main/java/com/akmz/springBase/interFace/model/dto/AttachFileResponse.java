package com.akmz.springBase.interFace.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor; // 이 줄을 추가합니다.

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor // 이 줄을 추가합니다.
@Schema(description = "첨부파일 조회 응답 DTO")
public class AttachFileResponse {

    @Schema(description = "파일 고유 ID", example = "1")
    private Long fileId;

    @Schema(description = "첨부 묶음 ID", example = "101")
    private Long attachId;

    @Schema(description = "원본 파일명", example = "example_document.pdf")
    private String originalFileName;

    @Schema(description = "파일 크기 (bytes)", example = "102400")
    private Long fileSize;

    @Schema(description = "업로드 시간", example = "2025-07-22T10:00:00")
    private LocalDateTime uploadedAt;

}
