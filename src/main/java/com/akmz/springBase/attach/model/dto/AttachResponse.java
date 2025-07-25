package com.akmz.springBase.attach.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "첨부 묶음 조회 응답 DTO")
public class AttachResponse {

    @Schema(description = "첨부 묶음 ID", example = "101")
    private Long attachId;

    @Schema(description = "첨부 묶음 이름", example = "프로젝트 보고서")
    private String attachName;

    @Schema(description = "생성자 ID", example = "user123")
    private String creatorId;

    @Schema(description = "생성 시간", example = "2025-07-23T10:00:00")
    private LocalDateTime createdAt;
}