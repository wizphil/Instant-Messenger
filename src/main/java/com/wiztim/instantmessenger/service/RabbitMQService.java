package com.wiztim.instantmessenger.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wiztim.instantmessenger.config.rabbitmq.MessageWrapper;
import com.wiztim.instantmessenger.config.rabbitmq.RabbitMQConfiguration;
import com.wiztim.instantmessenger.dto.GroupUserDTO;
import com.wiztim.instantmessenger.dto.MessageDTO;
import com.wiztim.instantmessenger.enums.OperationCode;
import com.wiztim.instantmessenger.persistence.user.UserInfo;
import com.wiztim.instantmessenger.dto.UserStatusDTO;
import com.wiztim.instantmessenger.persistence.Group;
import com.wiztim.instantmessenger.persistence.user.User;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Setter
@Component
public class RabbitMQService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitMQConfiguration rabbitMQConfiguration;

    ObjectMapper mapper = new ObjectMapper();


    protected void publishChatMessage(UUID toUserId, MessageDTO messageDTO) {
        publish(rabbitMQConfiguration.getDirectUserExchange(), toUserId.toString(), OperationCode.DirectMessage, messageDTO);
    }

    protected void publishUserTypingPing(UUID toUserId, UUID fromUserId) {
        publish(rabbitMQConfiguration.getDirectUserExchange(), toUserId.toString(), OperationCode.UserTypingPing, fromUserId);
    }

    protected void publishGroupTypingPing(UUID toUserId, GroupUserDTO groupUserDTO) {
        publish(rabbitMQConfiguration.getDirectUserExchange(), toUserId.toString(), OperationCode.GroupTypingPing, groupUserDTO);
    }

    protected void publishGroup(UUID userId, Group group) {
        publish(rabbitMQConfiguration.getDirectUserExchange(), userId.toString(), OperationCode.NewGroup, group);
    }

    protected void publishUserRemovedFromGroup(UUID toUserId, GroupUserDTO groupUserDTO) {
        publish(rabbitMQConfiguration.getDirectUserExchange(), toUserId.toString(), OperationCode.UserRemovedFromGroup, groupUserDTO);
    }

    protected void publishUser(User user) {
        publish(rabbitMQConfiguration.getFanoutUserExchange(), "", OperationCode.NewUser, user);
    }

    protected void publishUserInfo(UserInfo userInfo) {
        publish(rabbitMQConfiguration.getFanoutUserExchange(), "", OperationCode.UpdateUserInfo, userInfo);
    }

    protected void publishUserStatus(UserStatusDTO userStatusDTO) {
        publish(rabbitMQConfiguration.getFanoutUserExchange(), "", OperationCode.UpdateUserStatus, userStatusDTO);
    }

    protected void publishUserDisabled(UUID userId) {
        publish(rabbitMQConfiguration.getFanoutUserExchange(), "", OperationCode.DisableUser, userId);
    }

    private void publish(String exchange, String routingKey, OperationCode operation, Object object) {
        String payload;
        try {
            payload = mapper.writeValueAsString(object);
        } catch (IOException e) {
            log.error("Failed to convert to JSON. object=" + object);
            return;
        }

        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, new MessageWrapper(operation.getOpcode(), payload));
        } catch (Exception e) {
            log.error("Failed to send rabbitmq message. Exchange: " + exchange + "; routingKey: " + routingKey + "; operation: " + operation + "; object: " + object, e);
        }
    }
}
