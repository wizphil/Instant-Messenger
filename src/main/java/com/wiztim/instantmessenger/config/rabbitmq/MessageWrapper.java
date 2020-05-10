package com.wiztim.instantmessenger.config.rabbitmq;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageWrapper {
    String opcode;
    String payload;
}