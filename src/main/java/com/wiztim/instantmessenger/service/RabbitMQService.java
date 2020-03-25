package com.wiztim.instantmessenger.service;

import com.wiztim.instantmessenger.config.rabbitmq.GroupConfiguration;
import com.wiztim.instantmessenger.config.rabbitmq.MessageConfiguration;
import com.wiztim.instantmessenger.config.rabbitmq.UserConfiguration;
import com.wiztim.instantmessenger.dto.MessageDTO;
import com.wiztim.instantmessenger.persistence.user.UserInfo;
import com.wiztim.instantmessenger.dto.UserLeftGroupDTO;
import com.wiztim.instantmessenger.dto.UserStatusDTO;
import com.wiztim.instantmessenger.persistence.Group;
import com.wiztim.instantmessenger.persistence.user.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class RabbitMQService {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    MessageConfiguration messageConfiguration;

    @Autowired
    UserConfiguration userConfiguration;

    @Autowired
    GroupConfiguration groupConfiguration;


    protected void publishChatMessage(UUID toUserId, MessageDTO messageDTO) {
        publish(messageConfiguration.getExchange(), messageConfiguration.getChatPrefix() + toUserId, messageDTO);
    }

    protected void publishIsTypingPing(UUID toUserId, UUID fromUserId) {
        publish(messageConfiguration.getExchange(), messageConfiguration.getTypingPingPrefix() + toUserId, fromUserId);
    }

    protected void publishUser(User user) {
        publish(userConfiguration.getExchange(), userConfiguration.getCreateRoutingKey(), user);
    }

    protected void publishUserInfo(UserInfo userInfo) {
        publish(userConfiguration.getExchange(), userConfiguration.getInfoRoutingKey(), userInfo);
    }

    protected void publishUserStatus(UserStatusDTO userStatusDTO) {
        publish(userConfiguration.getExchange(), userConfiguration.getStatusRoutingKey(), userStatusDTO);
    }

    protected void publishUserDisabled(UUID userId) {
        publish(userConfiguration.getExchange(), userConfiguration.getDisabledRoutingKey(), userId);
    }

    protected void publishGroup(UUID userId, Group group) {
        publish(groupConfiguration.getExchange(), groupConfiguration.getCreatePrefix() + userId, group);
    }

    protected void publishUserRemovedFromGroup(UUID toUserId, UserLeftGroupDTO userLeftGroupDTO) {
        publish(groupConfiguration.getExchange(), groupConfiguration.getUserRemovedPrefix() + toUserId, userLeftGroupDTO);
    }

    private void publish(String exchange, String routingKey, Object object) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, object);
        } catch (Exception e) {
            log.error("Failed to send rabbitmq message. Exchange: " + exchange + "; routingKey: " + routingKey + "; object: " + object, e);
        }
    }
}
