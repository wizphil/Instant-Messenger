package com.wiztim.instantmessenger.repository;

import com.wiztim.instantmessenger.exceptions.DuplicateEntityException;
import com.wiztim.instantmessenger.exceptions.InvalidEntityException;
import com.wiztim.instantmessenger.persistence.Message;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.UUID;

@Component
public class MessageRepository {
    // TODO add  a database

    private final HashMap<UUID, Message> messages = new HashMap<>();

    public Message get(UUID id) {
        return messages.get(id);
    }

    // TODO need to be able to get messages: in conversation, in group, unread, and possibly more

    public void createMessage(Message message) {
        if (message == null || message.getId() == null) {
            throw new InvalidEntityException();
        }

        if (messageExists(message)) {
            throw new DuplicateEntityException(message.getId().toString());
        }

        messages.put(message.getId(), message);
    }

    public boolean messageExists(Message message) {
        if (message == null) {
            return false;
        }

        return messageExists(message.getId());
    }

    public boolean messageExists(UUID id) {
        return messages.containsKey(id);
    }
}
