package com.wizphil.instantmessenger.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wizphil.instantmessenger.dto.MessageWrapperDTO;
import lombok.extern.slf4j.Slf4j;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

@Slf4j
public class WebSocketEncoder implements Encoder.Text<MessageWrapperDTO>  {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String encode(MessageWrapperDTO messageWrapper) throws EncodeException {
        try {
            return objectMapper.writeValueAsString(messageWrapper);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object {}", messageWrapper, e);
        }

        return null;
    }

    @Override
    public void init(EndpointConfig endpointConfig) {
        // Custom initialization logic
    }

    @Override
    public void destroy() {
        // Close resources
    }
}
