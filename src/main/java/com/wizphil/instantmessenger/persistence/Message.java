package com.wizphil.instantmessenger.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@Builder
@Data
@CompoundIndexes({
        // 'to' is indexed so we can find unread messages (messages received while offline)
        @CompoundIndex(name = "time_idx", def = "{'to': 1, 'time': -1}"),
        // 'conversationId' is indexed so we can fetch a page of results
        @CompoundIndex(name = "conversation_idx", def = "{'conversationId': 1, 'time': -1}")
})
@Document
public class Message {
    @Id
    private String id;
    private String from;
    private String to;
    // this is a concatenation of 'from' and 'to', with the smaller value being first
    // this way we can index a conversation between two people off a single key
    private String conversationId;
    private String content;
    private Long time;
    // we don't actually delete messages, but we mark them as not enabled to prevent the client from receiving them
    private Boolean deleted;
}
