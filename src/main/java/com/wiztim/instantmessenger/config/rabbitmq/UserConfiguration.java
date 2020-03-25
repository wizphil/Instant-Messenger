package com.wiztim.instantmessenger.config.rabbitmq;

import lombok.Getter;
import lombok.Setter;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "user")
public class UserConfiguration {
    private String exchange;
    private String createRoutingKey;
    private String infoRoutingKey;
    private String statusRoutingKey;
    private String disabledRoutingKey;

    @Bean
    public TopicExchange userTopicExchange() {
        return new TopicExchange(exchange);
    }
}
