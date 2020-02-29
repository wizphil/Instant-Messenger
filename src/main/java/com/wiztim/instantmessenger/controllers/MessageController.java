package com.wiztim.instantmessenger.controllers;

import com.wiztim.instantmessenger.persistence.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.UUID;

@RestController
@RequestMapping("/v1/message")
@Slf4j
public class MessageController {
    private final HashMap<UUID, Message> messageMap = new HashMap<>();

    @GetMapping("/id")
    public UUID id() {
        return UUID.randomUUID();
    }

    @GetMapping("/{id}")
    public Message message(@PathVariable("id") UUID id)  {
        if (messageMap.containsKey(id)) {
            return messageMap.get(id);
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found.");
    }

    @PostMapping
    public UUID createMessage(@RequestBody Message message) {
        if (message.getId() == null) {
            message.setId(id());
        }

        if (messageMap.containsKey(message.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message with id: " + message.getId() + " already exists.");
        }

        messageMap.put(message.getId(), message);
        return message.getId();
    }

    @PutMapping("/{id}")
    public void updateMessage(@PathVariable("id") UUID id, @RequestBody Message message) {
        validateRequest(id);
        if (message == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be null.");
        }

        if (!id.equals(message.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path id does not match message id.");
        }

        messageMap.put(id, message);
    }

    @DeleteMapping("/{id}")
    public void deleteMessage(@PathVariable("id") UUID id) {
        validateRequest(id);
        messageMap.remove(id);
    }

    public void validateRequest(UUID id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id  cannot be null.");
        }

        if (!messageMap.containsKey(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message with id: " + id + " does not exist.");
        }
    }
}
