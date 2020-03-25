package com.wiztim.instantmessenger.config.rabbitmq;

import lombok.Getter;
import lombok.Setter;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "message")
public class MessageConfiguration {
    private String exchange;
    private String chatPrefix;
    private String typingPingPrefix;

    @Bean
    public DirectExchange messageDirectExchange() {
        return new DirectExchange(exchange);
    }
}
