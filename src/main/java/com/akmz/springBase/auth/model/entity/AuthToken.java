package com.akmz.springBase.auth.model.entity;

import lombok.Data;

import java.util.Date;

@Data
public class AuthToken {
    private Long id;
    private String token;
    private String userName;
    private String tokenType;
    private Date expiryDate;
    private Date createdDate;
    private boolean used;
}
