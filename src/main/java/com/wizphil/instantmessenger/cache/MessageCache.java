package com.wizphil.instantmessenger.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.wizphil.instantmessenger.persistence.GroupMessage;
import com.wizphil.instantmessenger.persistence.Message;
import com.wizphil.instantmessenger.repository.GroupMessageRepository;
import com.wizphil.instantmessenger.repository.MessageRepository;
import com.wizphil.instantmessenger.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// this isn't actually caching any messages, we just use it as an intermediate layer for talking to the database
// it would be nice to find a smart way to cache messages, so we don't have to read from the db on every login
@Component
@Slf4j
public class MessageCache {
    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private GroupMessageRepository groupMessageRepository;

    // This only keeps track of unread private messages
    // Unread group messages are currently not tracked
    private final SetMultimap<String, String> userUnreadMessages = Multimaps.synchronizedSetMultimap(HashMultimap.create());

    private final LoadingCache<String, Long> userUnreadMessageTimes = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public Long load(String conversationId) {
            return null;
        }
    });

    // TODO PAGE_SIZE should be a dynamic server config
    // using 250 because to prevent the request from ever being more than 500kb (messages have a 2kb character limit)
    private static final int MAX_RESULT_LIMIT = 250;

    public Message getMessage(String id) {
        return messageRepository.findById(id).orElse(null);
    }

    public GroupMessage getGroupMessage(String id) {
        return groupMessageRepository.findById(id).orElse(null);
    }

    public Page<Message> getConversationBeforeTime(String conversationId, long beforeTime) {
        // PageRequest is our way of calling 'limit' on the MongoDB query
        // feels weird, but we're always going to get page 0
        // we use beforeTime as our cursor, since the timestamp for a conversationId is always unique
        Pageable pageRequest = PageRequest.of(0, MAX_RESULT_LIMIT);
        return messageRepository.findConversationBeforeTime(conversationId, beforeTime, pageRequest);
    }

    public Page<GroupMessage> getGroupConversationBeforeTime(String groupId, long beforeTime) {
        // PageRequest is our way of calling 'limit' on the MongoDB query
        // feels weird, but we're always going to get page 0
        // we use beforeTime as our cursor, since the timestamp for a conversationId is always unique
        Pageable pageRequest = PageRequest.of(0, MAX_RESULT_LIMIT);
        return groupMessageRepository.findConversationBeforeTime(groupId, beforeTime, pageRequest);
    }

    public Message createMessage(Message message) {
        String toId = message.getTo();
        String fromId = message.getFrom();

        if (!userUnreadMessages.containsEntry(toId, fromId)) {
            userUnreadMessages.put(toId, fromId);
            userUnreadMessageTimes.put(toId + fromId, message.getTime());
        }

        // if they're creating a message, they must have seen the previous message
        markMessageAsSeen(fromId, toId);

        return messageRepository.insert(message);
    }

    public GroupMessage createGroupMessage(GroupMessage groupMessage) {
        return groupMessageRepository.insert(groupMessage);
    }

    // We should probably expand this to include the timestamp of the oldest unread message
    public Map<String, Long> getUnreadMessageCounts(String userId) {
        Map<String, Long> unreadMessageCounts = new HashMap<>();
        Pageable pageRequest = PageRequest.of(0, 1);
        Set<String> fromIds = userUnreadMessages.get(userId);
        for (String fromId : fromIds) {
            Long time;
            try {
                time = userUnreadMessageTimes.getUnchecked(userId + fromId);
            } catch (CacheLoader.InvalidCacheLoadException e) {
                log.error("getUnreadMessageCounts failes to find unread message time for user {} from user {}", userId, fromId);
                userUnreadMessages.remove(userId, fromId);
                continue;
            }

            String conversationId = MessageService.getConversationId(userId, fromId);
            Page<Message> unreadMessages = messageRepository.findMessagesToUserAfterTime(conversationId, time, pageRequest);
            long messageCount = unreadMessages.getTotalElements();
            unreadMessageCounts.put(fromId, messageCount);
        }

        return unreadMessageCounts;
    }

    public void markMessageAsSeen(String userId, String fromId) {
        userUnreadMessages.remove(userId, fromId);
        userUnreadMessageTimes.invalidate(userId + fromId);
    }

    public void markAllAsSeen(String userId) {
        Set<String> fromIds = userUnreadMessages.get(userId);
        for (String fromId : fromIds) {
            userUnreadMessageTimes.invalidate(userId + fromId);
        }

        userUnreadMessages.removeAll(userId);
    }
}
