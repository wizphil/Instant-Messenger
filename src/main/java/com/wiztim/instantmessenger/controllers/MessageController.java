package com.wiztim.instantmessenger.controllers;

import com.wiztim.instantmessenger.dto.MessageDTO;
import com.wiztim.instantmessenger.interfaces.IMessageController;
import com.wiztim.instantmessenger.persistence.Message;
import com.wiztim.instantmessenger.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/message")
@Slf4j
public class MessageController implements IMessageController {
    @Autowired
    MessageService messageService;

    @Override
    @GetMapping("/{id}")
    public Message getMessage(@PathVariable("id") UUID id)  {
        return messageService.getMessage(id);
    }

    @Override
    @PostMapping("from/{fromUserId}/toUser/{toUserId}")
    public void sendIsTypingToUser(@PathVariable("fromUserId") UUID fromUserId, @PathVariable("toUserId") UUID toUserId) {
        messageService.sendIsTypingToUser(fromUserId, toUserId);
    }

    @Override
    @PostMapping("from/{fromUserId}/toGroup/{toGroupId}")
    public void sendIsTypingToGroup(@PathVariable("fromUserId") UUID fromUserId, @PathVariable("toGroupId") UUID toGroupId) {
        messageService.sendIsTypingToGroup(fromUserId, toGroupId);
    }

    @Override
    @PostMapping("/user")
    public MessageDTO sendPrivateMessage(@RequestBody MessageDTO messageDTO) {
        return messageService.sendPrivateMessage(messageDTO);
    }

    @Override
    @PostMapping("/group")
    public MessageDTO sendGroupMessage(@RequestBody MessageDTO messageDTO) {
        return messageService.sendGroupMessage(messageDTO);
    }

}
