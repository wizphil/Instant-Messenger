package com.wiztim.instantmessenger.interfaces;

import com.wiztim.instantmessenger.dto.MessageDTO;
import com.wiztim.instantmessenger.exceptions.DuplicateEntityException;
import com.wiztim.instantmessenger.exceptions.InvalidEntityException;
import com.wiztim.instantmessenger.exceptions.MessageNotFoundException;
import com.wiztim.instantmessenger.exceptions.NullIdException;
import com.wiztim.instantmessenger.exceptions.RepositoryException;
import com.wiztim.instantmessenger.persistence.Message;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

public interface IMessageController {
    Message getMessage(@PathVariable("id") UUID id) throws NullIdException, MessageNotFoundException;

    void sendIsTypingToUser(@PathVariable("fromUserId") UUID fromUserId, @PathVariable("toUserId") UUID toUserId);

    void sendIsTypingToGroup(@PathVariable("fromUserId") UUID fromUserId, @PathVariable("toGroupId") UUID toGroupId);

    MessageDTO sendPrivateMessage(@RequestBody MessageDTO messageDTO) throws InvalidEntityException, DuplicateEntityException, RepositoryException, InvalidEntityException;

    MessageDTO sendGroupMessage(@RequestBody MessageDTO messageDTO) throws InvalidEntityException, DuplicateEntityException, RepositoryException, InvalidEntityException;
}
