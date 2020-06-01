package com.wizphil.instantmessenger.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wizphil.instantmessenger.enums.MessageCategory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class MessageWrapperDTO {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    MessageCategory category;
    String content;

    public MessageWrapperDTO(MessageCategory category, Object content) {
        this.category = category;
        try {
            this.content = objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object {}", content, e);
        }
    }
}