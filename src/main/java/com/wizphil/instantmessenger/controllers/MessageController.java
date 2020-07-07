package com.wizphil.instantmessenger.controllers;

import com.wizphil.instantmessenger.dto.MessageDTO;
import com.wizphil.instantmessenger.interfaces.IMessageController;
import com.wizphil.instantmessenger.persistence.GroupMessage;
import com.wizphil.instantmessenger.persistence.Message;
import com.wizphil.instantmessenger.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/message")
@Slf4j
public class MessageController implements IMessageController {
    @Autowired
    MessageService messageService;

    @Override
    @GetMapping("/{id}")
    public Message getMessage(@PathVariable("id") String id) {
        return messageService.getMessage(id);
    }

    @Override
    @GetMapping("/conversation/user1/{user1Id}/user2/{user2Id}/before/{time}")
    public Page<Message> getConversation(@PathVariable("user1Id") String user1Id, @PathVariable("user2Id") String user2Id, @PathVariable("time") long time) {
        return messageService.getPrivateChatConversation(user1Id, user2Id, time);
    }

    @Override
    @GetMapping("/conversation/group/{groupId}/before/{time}")
    public Page<GroupMessage> getConversation(@PathVariable("groupId") String groupId, @PathVariable("time") long time) {
        return messageService.getGroupChatConversation(groupId, time);
    }

    @Override
    @GetMapping("/group/{id}")
    public GroupMessage getGroupMessage(@PathVariable("id") String id)  {
        return messageService.getGroupMessage(id);
    }

    @Override
    @PostMapping("from/{fromUserId}/toUser/{toUserId}")
    public void sendIsTypingToUser(@PathVariable("fromUserId") String fromId, @PathVariable("toUserId") String toId) {
        messageService.sendIsTypingToUser(fromId, toId);
    }

    @Override
    @PostMapping("from/{fromUserId}/toGroup/{toGroupId}")
    public void sendIsTypingToGroup(@PathVariable("fromUserId") String fromId, @PathVariable("toGroupId") String groupId) {
        messageService.sendIsTypingToGroup(fromId, groupId);
    }

    @Override
    @PostMapping("/user")
    public String sendPrivateMessage(@RequestBody MessageDTO messageDTO) {
        return messageService.sendPrivateMessage(messageDTO);
    }

    @Override
    @PostMapping("/group")
    public GroupMessage sendGroupMessage(@RequestBody MessageDTO messageDTO) {
        return messageService.sendGroupMessage(messageDTO);
    }

    @Override
    @GetMapping("/unread/user/{userId}")
    public Map<String, Long> getUnreadMessageCounts(String userId) {
        return messageService.getUnreadMessageCounts(userId);
    }

    @Override
    @DeleteMapping("/unread/user/{userId}/from/{fromId}")
    public void markMessageAsSeen(String userId, String fromId) {
        messageService.markMessageAsSeen(userId, fromId);
    }

    @Override
    @DeleteMapping("/unread/user/{userId}")
    public void markAllAsSeen(String userId) {
        messageService.markAllAsSeen(userId);
    }

}
