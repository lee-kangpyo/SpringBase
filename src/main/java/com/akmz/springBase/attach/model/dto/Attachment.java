package com.akmz.springBase.attach.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Attachment {
    private String filename;
    private byte[] content;
    private String contentType;
}
