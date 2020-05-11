package com.wiztim.instantmessenger.service;

import com.wiztim.instantmessenger.dto.GroupUserDTO;
import com.wiztim.instantmessenger.dto.MessageDTO;
import com.wiztim.instantmessenger.dto.MessageWrapperDTO;
import com.wiztim.instantmessenger.enums.MessageCategory;
import com.wiztim.instantmessenger.exceptions.DuplicateEntityException;
import com.wiztim.instantmessenger.exceptions.InvalidEntityException;
import com.wiztim.instantmessenger.exceptions.MessageNotFoundException;
import com.wiztim.instantmessenger.exceptions.NullIdException;
import com.wiztim.instantmessenger.exceptions.RepositoryException;
import com.wiztim.instantmessenger.persistence.Group;
import com.wiztim.instantmessenger.persistence.Message;
import com.wiztim.instantmessenger.repository.MessageRepository;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@Setter
@Slf4j
public class MessageService {
    @Autowired
    MessageRepository messageRepository;

    @Autowired
    UserService userService;

    @Autowired
    GroupService groupService;

    @Autowired
    private SessionService sessionService;

    // System messages will have an all 0 id
    // currently this is only used when a user leaves a group
    private static final UUID systemId = new UUID(0, 0);

    public Message getMessage(UUID id) {
        return validateAndGetMessage(id);
    }

    public MessageDTO sendPrivateMessage(MessageDTO messageDTO) {
        validateMessage(messageDTO);

        UUID userId = messageDTO.getTo();
        userService.validateEnabledUser(userId);

        messageDTO.setTime(Instant.now().toEpochMilli());
        saveMessage(messageDTO);

        if (!userService.isUserOffline(userId)) {
            sessionService.sendMessageToUser(userId, new MessageWrapperDTO(MessageCategory.DirectMessage, messageDTO));
        }

        return messageDTO;
    }

    public MessageDTO sendGroupMessage(MessageDTO messageDTO) {
        validateMessage(messageDTO);

        Group group = groupService.validateAndGetGroup(messageDTO.getTo());

        messageDTO.setTime(Instant.now().toEpochMilli());
        saveMessage(messageDTO);

        List<UUID> onlineUserIds = userService.getOnlineUserIds(group.getUserIds());
        onlineUserIds.remove(messageDTO.getFrom());

        sessionService.sendMessageToUsers(onlineUserIds, new MessageWrapperDTO(MessageCategory.DirectMessage, messageDTO));

        return messageDTO;
    }

    public void sendIsTypingToUser(UUID fromUserId, UUID toUserId) {
        if (fromUserId == null || toUserId == null) {
            throw new NullIdException();
        }

        // check if both users are online
        if (userService.isUserOffline(fromUserId) || userService.isUserOffline(toUserId)) {
            return;
        }

        sessionService.sendMessageToUser(toUserId, new MessageWrapperDTO(MessageCategory.UserTypingPing, fromUserId));
    }

    public void sendIsTypingToGroup(UUID fromUserId, UUID toGroupId) {
        if (fromUserId == null || toGroupId == null) {
            throw new NullIdException();
        }

        // check if user is online
        if (userService.isUserOffline(fromUserId)) {
            return;
        }

        Group group = groupService.validateAndGetGroup(toGroupId);

        GroupUserDTO groupUserDTO = GroupUserDTO.builder()
                .userId(fromUserId)
                .groupId(toGroupId)
                .build();

        List<UUID> onlineUserIds = userService.getOnlineUserIds(group.getUserIds());
        onlineUserIds.remove(fromUserId);

        sessionService.sendMessageToUsers(onlineUserIds, new MessageWrapperDTO(MessageCategory.GroupTypingPing, groupUserDTO));
    }

    private void saveMessage(MessageDTO messageDTO) {
        Message message = Message.builder()
                .id(UUID.randomUUID())
                .from(messageDTO.getFrom())
                .to(messageDTO.getTo())
                .content(messageDTO.getContent())
                .time(messageDTO.getTime())
                .build();

        // surely this can't happen
        if (messageRepository.messageExists(message.getId())) {
            throw new DuplicateEntityException(message.getId());
        }

        try {
            messageRepository.createMessage(message);
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    private Message validateAndGetMessage(UUID id) {
        if (id == null || !messageRepository.messageExists(id)) {
            throw new MessageNotFoundException(id);
        }

        return messageRepository.get(id);
    }

    private void validateMessage(MessageDTO messageDTO) {
        if (messageDTO == null  || messageDTO.getFrom() == null || messageDTO.getTo() == null || messageDTO.getContent() == null
                || messageDTO.getContent().isBlank() || messageDTO.getTo().equals(messageDTO.getFrom()) || messageDTO.getTo().equals(systemId)) {
            throw new InvalidEntityException();
        }

        // if sender isn't system, then make sure sender is a valid user
        if (!messageDTO.getFrom().equals(systemId)) {
            userService.validateEnabledUser(messageDTO.getFrom());
        }
    }
}
