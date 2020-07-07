package com.wizphil.instantmessenger.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.wizphil.instantmessenger.dto.GroupUserDTO;
import com.wizphil.instantmessenger.dto.MessageDTO;
import com.wizphil.instantmessenger.dto.MessageWrapperDTO;
import com.wizphil.instantmessenger.enums.MessageCategory;
import com.wizphil.instantmessenger.exceptions.InvalidEntityException;
import com.wizphil.instantmessenger.exceptions.MessageTooLargeException;
import com.wizphil.instantmessenger.exceptions.NullIdException;
import com.wizphil.instantmessenger.exceptions.UserNotInGroupException;
import com.wizphil.instantmessenger.persistence.Group;
import com.wizphil.instantmessenger.persistence.GroupMessage;
import com.wizphil.instantmessenger.persistence.Message;
import com.wizphil.instantmessenger.cache.MessageCache;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Setter
@Slf4j
public class MessageService {
    @Autowired
    MessageCache messageCache;

    @Autowired
    UserService userService;

    @Autowired
    GroupService groupService;

    @Autowired
    private SessionService sessionService;

    // Messages have a compound index using the properties {to, time}
    // We want to ensure that each {to, time} is unique
    // If the user is sending several messages at the same time, some could fall on the same millisecond
    // This will give a time as close the current time as possible without duplicates.
    // https://stackoverflow.com/questions/9191288/creating-a-unique-timestamp-in-java/9191383#9191383
    private final LoadingCache<String, AtomicLong> lastMessageTimeCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
        @Override
        public AtomicLong load(String id) {
            return new AtomicLong(System.currentTimeMillis());
        }
    });

    public long uniqueCurrentTimeMS(String id) {
        long now = System.currentTimeMillis();
        while(true) {
            long lastTime = lastMessageTimeCache.getUnchecked(id).get();
            if (lastTime >= now) {
                now = lastTime + 1;
            }

            if (lastMessageTimeCache.getUnchecked(id).compareAndSet(lastTime, now)) {
                return now;
            }
        }
    }

    // System messages will have an all 0 id
    // currently this is only used when a user joins/leaves a group
    private static final String systemId = "system-generated-message";
    // TODO MAX_MESSAGE_SIZE should be a dynamic server config
    private static final int MAX_MESSAGE_SIZE = 2000;

    public Message getMessage(String id) {
        return validateAndGetMessage(id);
    }

    public GroupMessage getGroupMessage(String id) {
        return validateAndGetGroupMessage(id);
    }

    public Page<Message> getPrivateChatConversation(String user1Id, String user2Id, long beforeTime) {
        // beforeTime <= 0 means get most recent messages
        // it means the user doesn't have any messages and needs to get the first page
        if (beforeTime <= 0) {
            beforeTime = System.currentTimeMillis();
        }

        String conversationId = getConversationId(user1Id, user2Id);
        return messageCache.getConversationBeforeTime(conversationId, beforeTime);
    }

    public Page<GroupMessage> getGroupChatConversation(String groupId, long beforeTime) {
        // beforeTime <= 0 means get most recent messages
        // it means the user doesn't have any messages and needs to get the first page
        if (beforeTime <= 0) {
            beforeTime = System.currentTimeMillis();
        }

        return messageCache.getGroupConversationBeforeTime(groupId, beforeTime);
    }

    public String sendPrivateMessage(MessageDTO messageDTO) {
        // validateMessage will validate fromId
        validateMessage(messageDTO);

        // validating toId is done here, because a Message can be for a user or for a group
        String toId = messageDTO.getTo();
        String fromId = messageDTO.getFrom();
        userService.validateUserEnabled(toId);

        String conversationId = getConversationId(messageDTO.getFrom(), toId);
        long time = uniqueCurrentTimeMS(conversationId);
        messageDTO.setTime(time);

        Message message = Message.builder()
                .from(messageDTO.getFrom())
                .to(messageDTO.getTo())
                .conversationId(conversationId)
                .content(messageDTO.getContent())
                .time(time)
                .deleted(false)
                .build();

        message = messageCache.createMessage(message);

        sessionService.sendMessageToUser(toId, new MessageWrapperDTO(MessageCategory.DirectMessage, message));

        // send to yourself in case there are other sessions open
        sessionService.sendMessageToUser(fromId, new MessageWrapperDTO(MessageCategory.DirectMessage, message));

        return message.getId();
    }

    public GroupMessage sendGroupMessage(MessageDTO messageDTO) {
        // validateMessage will validate fromId
        validateMessage(messageDTO);

        // validating toId is done here, because a Message can be for a user or for a group
        Group group = groupService.getExistingGroup(messageDTO.getTo());

        long time = uniqueCurrentTimeMS(group.getId());
        GroupMessage groupMessage = GroupMessage.builder()
                .from(messageDTO.getFrom())
                .groupId(group.getId())
                .content(messageDTO.getContent())
                .time(time)
                .build();

        groupMessage = messageCache.createGroupMessage(groupMessage);

        sessionService.sendMessageToUsers(group.getUserIds(), new MessageWrapperDTO(MessageCategory.GroupMessage, groupMessage));

        return groupMessage;
    }

    public void sendIsTypingToUser(String fromUserId, String toUserId) {
        if (fromUserId == null || toUserId == null) {
            throw new NullIdException();
        }

        // no need to validate if user is online or if they exist
        // if they have no sessions, this will do nothing
        sessionService.sendMessageToUser(toUserId, new MessageWrapperDTO(MessageCategory.UserTypingPing, fromUserId));
    }

    public void sendIsTypingToGroup(String userId, String groupId) {
        if (userId == null || groupId == null) {
            throw new NullIdException();
        }

        Group group = groupService.getExistingGroup(groupId);
        Set<String> userIds = group.getUserIds();

        if (!userIds.contains(userId)) {
            throw new UserNotInGroupException(groupId, userId);
        }

        GroupUserDTO groupUserDTO = GroupUserDTO.builder()
                .userId(userId)
                .groupId(groupId)
                .build();

        // don't send isTyping to ourselves
        userIds.remove(userId);

        sessionService.sendMessageToUsers(userIds, new MessageWrapperDTO(MessageCategory.GroupTypingPing, groupUserDTO));
    }

    public Map<String, Long> getUnreadMessageCounts(String userId) {
        return messageCache.getUnreadMessageCounts(userId);
    }

    public void markMessageAsSeen(String userId, String fromId) {
        messageCache.markMessageAsSeen(userId, fromId);
    }

    public void markAllAsSeen(String userId) {
        messageCache.markAllAsSeen(userId);
    }

    private Message validateAndGetMessage(String id) {
        if (id == null) {
            throw new NullIdException();
        }

        return messageCache.getMessage(id);
    }

    private GroupMessage validateAndGetGroupMessage(String id) {
        if (id == null) {
            throw new NullIdException();
        }

        return messageCache.getGroupMessage(id);
    }

    private void validateMessage(MessageDTO messageDTO) {
        if (messageDTO == null  || messageDTO.getFrom() == null || messageDTO.getTo() == null || messageDTO.getContent() == null
                || messageDTO.getContent().isBlank() || messageDTO.getTo().equals(messageDTO.getFrom()) || messageDTO.getTo().equals(systemId)) {
            throw new InvalidEntityException();
        }

        int messageSize = messageDTO.getContent().length();
        if (messageSize > MAX_MESSAGE_SIZE) {
            throw new MessageTooLargeException(messageSize, MAX_MESSAGE_SIZE);
        }

        // if sender isn't system, then make sure sender is a valid user
        if (!messageDTO.getFrom().equals(systemId)) {
            userService.validateUserEnabled(messageDTO.getFrom());
        }
    }

    public static String getConversationId(String fromId, String toId) {
        if (fromId.compareTo(toId) < 0) {
            return fromId + toId;
        } else {
            return toId + fromId;
        }
    }
}
