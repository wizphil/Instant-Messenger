package com.wizphil.instantmessenger.interfaces;

import com.wizphil.instantmessenger.dto.MessageDTO;
import com.wizphil.instantmessenger.exceptions.DuplicateEntityException;
import com.wizphil.instantmessenger.exceptions.InvalidEntityException;
import com.wizphil.instantmessenger.exceptions.NullIdException;
import com.wizphil.instantmessenger.exceptions.RepositoryException;
import com.wizphil.instantmessenger.persistence.GroupMessage;
import com.wizphil.instantmessenger.persistence.Message;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

public interface IMessageController {
    Message getMessage(@PathVariable("id") String id) throws NullIdException;

    GroupMessage getGroupMessage(@PathVariable("id") String id) throws NullIdException;

    Page<Message> getConversation(@PathVariable("user1Id") String user1Id, @PathVariable("user2Id") String user2Id, @PathVariable("time") long time);

    Page<GroupMessage> getConversation(@PathVariable("groupId") String groupId, @PathVariable("time") long time);

    void sendIsTypingToUser(@PathVariable("fromId") String fromId, @PathVariable("toId") String toId);

    void sendIsTypingToGroup(@PathVariable("fromId") String fromId, @PathVariable("groupId") String groupId);

    String sendPrivateMessage(@RequestBody MessageDTO messageDTO) throws InvalidEntityException, DuplicateEntityException, RepositoryException, InvalidEntityException;

    GroupMessage sendGroupMessage(@RequestBody MessageDTO messageDTO) throws InvalidEntityException, DuplicateEntityException, RepositoryException, InvalidEntityException;

    Map<String, Long> getUnreadMessageCounts(@PathVariable("userId") String userId);

    void markMessageAsSeen(@PathVariable("userId") String userId, @PathVariable("fromId")  String fromId);

    void markAllAsSeen(@PathVariable("userId") String userId);
}
