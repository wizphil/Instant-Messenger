package com.wizphil.instantmessenger.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MessageDTO {
    private String from;
    private String to;
    private String content;
    private long time;
    private boolean deleted;
}
