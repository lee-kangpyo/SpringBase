package com.akmz.springBase.admin.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserAdminViewResponse {
    private String userName;
    private String email;
    private String useYn;
    private int loginFailureCount;
    private LocalDateTime lastFailureTimestamp;
}
