package com.wizphil.instantmessenger.service;

import com.wizphil.instantmessenger.dto.MessageDTO;
import com.wizphil.instantmessenger.persistence.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
public class MessageServiceTests {
    @Autowired
    MessageService service;

    private static final String ALICE = "alice";
    private static final String BOB = "bob";

    //@Test
    public void createsMessage() {

        Message helloWorld = service.sendPrivateMessage(MessageDTO.builder()
                .from(BOB)
                .to(ALICE)
                .content("Hello world!")
                .build());

        assertThat(helloWorld).isNotNull();
        assertThat(helloWorld.getId()).isNotNull().isNotBlank();
        assertThat(helloWorld.getTime()).isGreaterThan(0);
        assertThat(helloWorld.getDeleted()).isFalse();
    }

    //@Test
    public void getsMessage() {

        Message message = service.sendPrivateMessage(MessageDTO.builder()
                .from(ALICE)
                .to(BOB)
                .content("This is a message.")
                .build());

        Message result = service.getMessage(message.getId());

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(message.getContent());
        assertThat(result.getTime()).isGreaterThan(0);
        assertThat(result.getDeleted()).isFalse();
    }

    //@Test
    public void sendAndGetsPrivateChatConversation() {
        String jerry = "jerry";
        String george = "george";
        String elaine = "elaine";

        Message shirtButton = service.sendPrivateMessage(MessageDTO.builder()
                .from(jerry)
                .to(george)
                .content("See, now, to me, that button is in the worst possible spot.")
                .build());

        Message really = service.sendPrivateMessage(MessageDTO.builder()
                .from(george)
                .to(jerry)
                .content("Really?")
                .build());

        Message keyButton = service.sendPrivateMessage(MessageDTO.builder()
                .from(jerry)
                .to(george)
                .content("Oh yeah. The second button is the key button. It literally makes or breaks the shirt. Look at it: it's too high, it's in no-man's land.")
                .build());

        Message saturday = service.sendPrivateMessage(MessageDTO.builder()
                .from(jerry)
                .to(elaine)
                .content("Why do I always have the feeling that everybody's doing something better than me on Saturday afternoons?")
                .build());

        Message diner = service.sendPrivateMessage(MessageDTO.builder()
                .from(george)
                .to(elaine)
                .content("So how long did you live there?")
                .build());

        Page<Message> conversation = service.getPrivateChatConversation(jerry, george, 0);

        assertThat(conversation).isNotNull();
        assertThat(conversation.getTotalElements()).isEqualTo(3);

        int i = 0;
        // the returned message order should be oldest -> newest
        List<String> messageIds = List.of(keyButton.getId(), really.getId(), shirtButton.getId());
        for (Message message : conversation) {
            assertThat(message.getId()).isEqualTo(messageIds.get(i));
            i++;
        }
    }

    //@Test
    public void getsUnreadMessageCountsAndMarksAsSeen() {
        String phil = "phil-service";
        String tim = "tim-service";
        String brian = "brian-service";


        Message fromTimV1 = service.sendPrivateMessage(MessageDTO.builder()
                .from(tim)
                .to(phil)
                .content("Oi Phil.")
                .build());

        // verify we haven't seen the new message
        Map<String, Long> oneUnreadMessage = service.getUnreadMessageCounts(phil);

        assertThat(oneUnreadMessage.size()).isEqualTo(1);
        assertThat(oneUnreadMessage.containsKey(tim)).isTrue();
        assertThat(oneUnreadMessage.get(tim)).isEqualTo(1);

        // verify we've seen the message
        service.markMessageAsSeen(phil, tim);
        Map<String, Long> zeroUnreadMessages = service.getUnreadMessageCounts(phil);
        assertThat(zeroUnreadMessages.size()).isEqualTo(0);

        // get some more messages
        Message fromTimV2 = service.sendPrivateMessage(MessageDTO.builder()
                .from(tim)
                .to(phil)
                .content("Are you done with that messenger yet?")
                .build());

        Message fromTimV2Again = service.sendPrivateMessage(MessageDTO.builder()
                .from(tim)
                .to(phil)
                .content("Please start sleeping normal.")
                .build());

        Message fromBrian = service.sendPrivateMessage(MessageDTO.builder()
                .from(brian)
                .to(phil)
                .content("ADT")
                .build());

        // verify we havent seen any of the conversations
        Map<String, Long> threeUnreadMessages = service.getUnreadMessageCounts(phil);

        assertThat(threeUnreadMessages.size()).isEqualTo(2);
        assertThat(threeUnreadMessages.containsKey(tim)).isTrue();
        assertThat(threeUnreadMessages.containsKey(brian)).isTrue();
        assertThat(threeUnreadMessages.get(tim)).isEqualTo(2);
        assertThat(threeUnreadMessages.get(brian)).isEqualTo(1);

        // mark tim's message as seen again
        service.markMessageAsSeen(phil, tim);

        // verify that we've seen tim's message, but haven't seen brian's
        Map<String, Long> oneUnreadMessageFromBrian = service.getUnreadMessageCounts(phil);

        assertThat(oneUnreadMessageFromBrian.size()).isEqualTo(1);
         assertThat(oneUnreadMessageFromBrian.containsKey(brian)).isTrue();
        assertThat(oneUnreadMessageFromBrian.get(brian)).isEqualTo(1);

        // add tim's message again (no need to verify this time around)
        Message fromTimV3 = service.sendPrivateMessage(MessageDTO.builder()
                .from(tim)
                .to(phil)
                .content("Goodbye.")
                .build());

        // verify we have no unread messages if we've marked all as seen
        service.markAllAsSeen(phil);
        Map<String, Long> zeroUnreadMessagesAgain = service.getUnreadMessageCounts(phil);
        assertThat(zeroUnreadMessagesAgain.size()).isEqualTo(0);
    }
}
