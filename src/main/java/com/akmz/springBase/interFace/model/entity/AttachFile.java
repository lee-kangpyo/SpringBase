package com.akmz.springBase.interFace.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AttachFile {
    private Long fileId;
    private Long attachId;
    private String originalFileName;
    private String savedFileName;
    private String filePath;
    private Long fileSize;
    private String uploaderId;
    private LocalDateTime uploadedAt;
    private String status;
    private LocalDateTime deletedAt;
}
