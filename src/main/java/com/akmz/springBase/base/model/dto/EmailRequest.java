package com.akmz.springBase.base.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class EmailRequest {
    private String to;
    private String subject;
    private String text;
    private String[] cc;
    private String[] bcc;
    private boolean isHtml;
    private List<Attachment> attachments;
}
