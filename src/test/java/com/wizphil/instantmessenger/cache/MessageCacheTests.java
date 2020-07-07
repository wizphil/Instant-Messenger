package com.wizphil.instantmessenger.cache;

import com.wizphil.instantmessenger.persistence.Message;
import com.wizphil.instantmessenger.service.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
@WebAppConfiguration
public class MessageCacheTests {
    @Autowired
    MessageCache cache;

    private static final String ALICE = "alice";
    private static final String BOB = "bob";

    @Test
    public void createsMessage() {

        Message helloWorld = cache.createMessage(Message.builder()
                .from(BOB)
                .to(ALICE)
                .conversationId(MessageService.getConversationId(BOB, ALICE))
                .content("Hello world!")
                .time(System.currentTimeMillis())
                .deleted(false)
                .build());

        assertThat(helloWorld).isNotNull();
        assertThat(helloWorld.getId());
    }

    @Test
    public void getsMessage() {

        Message message = cache.createMessage(Message.builder()
                .from(ALICE)
                .to(BOB)
                .conversationId(MessageService.getConversationId(BOB, ALICE))
                .content("This is a message.")
                .time(System.currentTimeMillis())
                .deleted(false)
                .build());

        Message result = cache.getMessage(message.getId());

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(message.getContent());
    }

    @Test
    public void getsConversationBeforeTime() {
        String user1Id = "user1Id";
        String user2Id = "user2Id";
        String user3Id = "user3Id";

        Message someoneSaid = cache.createMessage(Message.builder()
                .from(user1Id)
                .to(user2Id)
                .conversationId(MessageService.getConversationId(user1Id, user2Id))
                .content("Someone said you look like an owl.")
                .time(System.currentTimeMillis() - 1000L)
                .deleted(false)
                .build());

        Message who = cache.createMessage(Message.builder()
                .from(user2Id)
                .to(user1Id)
                .conversationId(MessageService.getConversationId(user2Id, user1Id))
                .content("Who?")
                .time(System.currentTimeMillis() - 900L)
                .deleted(false)
                .build());

        Message univeralRemote = cache.createMessage(Message.builder()
                .from(user3Id)
                .to(user2Id)
                .conversationId(MessageService.getConversationId(user3Id, user2Id))
                .content("I remember the first time I saw a universal TV remote. I said to myself, 'This changes everything.'")
                .time(System.currentTimeMillis() - 1000L)
                .deleted(false)
                .build());

        String conversationId = MessageService.getConversationId(user1Id, user2Id);
        Page<Message> conversation = cache.getConversationBeforeTime(conversationId, System.currentTimeMillis());

        assertThat(conversation).isNotNull();
        assertThat(conversation.getTotalElements()).isEqualTo(2);
    }

    @Test
    public void getsUnreadMessageCountsAndMarksAsSeen() {
        String phil = "phil";
        String tim = "tim";
        String brian = "brian";

        // get a new unread message
        Message fromTimV1 = cache.createMessage(Message.builder()
                .from(tim)
                .to(phil)
                .conversationId(MessageService.getConversationId(tim, phil))
                .content("Oi Phil.")
                .time(System.currentTimeMillis() - 1000L)
                .deleted(false)
                .build());

        // verify we haven't seen the new message
        Map<String, Long> oneUnreadMessage = cache.getUnreadMessageCounts(phil);

        assertThat(oneUnreadMessage.size()).isEqualTo(1);
        assertThat(oneUnreadMessage.containsKey(tim)).isTrue();
        assertThat(oneUnreadMessage.get(tim)).isEqualTo(1);

        // verify we've seen the message
        cache.markMessageAsSeen(phil, tim);
        Map<String, Long> zeroUnreadMessages = cache.getUnreadMessageCounts(phil);
        assertThat(zeroUnreadMessages.size()).isEqualTo(0);

        // get some more messages
        Message fromTimV2 = cache.createMessage(Message.builder()
                .from(tim)
                .to(phil)
                .conversationId(MessageService.getConversationId(tim, phil))
                .content("Are you done with that messenger yet?")
                .time(System.currentTimeMillis() - 900L)
                .deleted(false)
                .build());

        Message fromTimV2Again = cache.createMessage(Message.builder()
                .from(tim)
                .to(phil)
                .conversationId(MessageService.getConversationId(tim, phil))
                .content("Please start sleeping normal.")
                .time(System.currentTimeMillis() - 800L)
                .deleted(false)
                .build());

        Message fromBrian = cache.createMessage(Message.builder()
                .from(brian)
                .to(phil)
                .conversationId(MessageService.getConversationId(brian, phil))
                .content("ADT")
                .time(System.currentTimeMillis() - 1000L)
                .deleted(false)
                .build());

        // verify we havent seen any of the conversations
        Map<String, Long> threeUnreadMessages = cache.getUnreadMessageCounts(phil);

        assertThat(threeUnreadMessages.size()).isEqualTo(2);
        assertThat(threeUnreadMessages.containsKey(tim)).isTrue();
        assertThat(threeUnreadMessages.containsKey(brian)).isTrue();
        assertThat(threeUnreadMessages.get(tim)).isEqualTo(2);
        assertThat(threeUnreadMessages.get(brian)).isEqualTo(1);

        // mark tim's message as seen again
        cache.markMessageAsSeen(phil, tim);

        // verify that we've seen tim's message, but haven't seen brian's
        Map<String, Long> oneUnreadMessageFromBrian = cache.getUnreadMessageCounts(phil);

        assertThat(oneUnreadMessageFromBrian.size()).isEqualTo(1);
         assertThat(oneUnreadMessageFromBrian.containsKey(brian)).isTrue();
        assertThat(oneUnreadMessageFromBrian.get(brian)).isEqualTo(1);

        // add tim's message again (no need to verify this time around)
        Message fromTimV3 = cache.createMessage(Message.builder()
                .from(tim)
                .to(phil)
                .conversationId(MessageService.getConversationId(tim, phil))
                .content("Goodbye.")
                .time(System.currentTimeMillis() - 700L)
                .deleted(false)
                .build());

        // verify we have no unread messages if we've marked all as seen
        cache.markAllAsSeen(phil);
        Map<String, Long> zeroUnreadMessagesAgain = cache.getUnreadMessageCounts(phil);
        assertThat(zeroUnreadMessagesAgain.size()).isEqualTo(0);
    }
}
