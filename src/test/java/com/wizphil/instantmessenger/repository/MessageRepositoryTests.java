package com.wizphil.instantmessenger.repository;

import com.wizphil.instantmessenger.persistence.Message;
import com.wizphil.instantmessenger.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
@WebAppConfiguration
public class MessageRepositoryTests {
    @Autowired
    MessageRepository repository;

    Message beginningOfTime, currentTime, someTimeAgo, anotherMessage;

    private static final String user1 = "alice";
    private static final String user2 = "bob";
    private static final String user3 = "sage";

    @BeforeEach
    public void setup() {
        repository.deleteAll();
        beginningOfTime = repository.save(Message.builder()
                .from(user1)
                .to(user2)
                .conversationId(MessageService.getConversationId(user1, user2))
                .content("Hello world!")
                .time(0L)
                .deleted(false)
                .build());

        currentTime = repository.save(Message.builder()
                .from(user2)
                .to(user1)
                .conversationId(MessageService.getConversationId(user2, user1))
                .content("it is now my dudes")
                .time(System.currentTimeMillis())
                .deleted(false)
                .build());

        someTimeAgo = repository.save(Message.builder()
                .from(user1)
                .to(user2)
                .conversationId(MessageService.getConversationId(user1, user2))
                .content("In a galaxy far, far away.")
                .time(System.currentTimeMillis() - 10000L)
                .deleted(false)
                .build());

        anotherMessage = repository.save(Message.builder()
                .from(user3)
                .to(user2)
                .conversationId(MessageService.getConversationId(user3, user2))
                .content("You are a boulder, I am a mountain.")
                .time(System.currentTimeMillis() - 1000L)
                .deleted(false)
                .build());
    }

    @Test
    public void setsIdOnSave() {

        Message message = repository.save(Message.builder()
                .from(user1)
                .to(user2)
                .conversationId(MessageService.getConversationId(user1, user2))
                .content("Be sure to drink your ovaltine.")
                .time(System.currentTimeMillis())
                .deleted(false)
                .build());

        assertThat(message).isNotNull();
        assertThat(message.getId()).isNotNull();
    }

    @Test
    public void findsConversationBeforeTimeNow() {

        String conversationId = MessageService.getConversationId(user1, user2);
        Pageable pageRequest = PageRequest.of(0, 250);
        Page<Message> messages = repository.findConversationBeforeTime(conversationId, System.currentTimeMillis(), pageRequest);

        assertThat(messages).isNotNull();
        assertThat(messages.getTotalElements()).isEqualTo(3);
    }

    @Test
    public void findsMessagesToUserAfterTime() {

        String conversationId = MessageService.getConversationId(user1, user2);
        Pageable pageRequest = PageRequest.of(0, 1);
        Page<Message> messages = repository.findMessagesToUserAfterTime(conversationId, System.currentTimeMillis() - 50000L, pageRequest);

        assertThat(messages).isNotNull();
        assertThat(messages.getTotalElements()).isEqualTo(2);
    }
}
