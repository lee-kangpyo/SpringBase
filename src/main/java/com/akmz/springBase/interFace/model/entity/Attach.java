package com.akmz.springBase.interFace.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Attach {
    private Long attachId;
    private String attachName;
    private String creatorId;
    private LocalDateTime createdAt;
}
