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
@ConfigurationProperties(prefix = "group")
public class GroupConfiguration {
    private String exchange;
    private String createPrefix;
    private String userRemovedPrefix;

    @Bean
    public DirectExchange groupDirectExchange() {
        return new DirectExchange(exchange);
    }
}
