package com.wiztim.instantmessenger.config.rabbitmq;

import lombok.Getter;
import lombok.Setter;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMQConfiguration {
    private String directUserExchange;
    private String fanoutUserExchange;

    // used for directly messaging a user, routing key will be user's id
    @Bean
    public DirectExchange directUserExchange() {
        return new DirectExchange(directUserExchange, true, false);
    }

    // used for sending user updates to all online users
    // this includes status changes and name/info changes
    @Bean
    public FanoutExchange fanoutUserExchange() {
        return new FanoutExchange(fanoutUserExchange, true, false);
    }
}
