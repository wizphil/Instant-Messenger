package com.wizphil.instantmessenger.persistence;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Builder
@Data
@CompoundIndexes({
        // 'to' is indexed so we can find unread messages (messages received while offline)
        @CompoundIndex(name = "time_idx", def = "{'groupId': 1, 'time': -1}")
})
@Document
public class GroupMessage {
    @Id
    private String id;
    private String from;
    private String groupId;
    private String content;
    private Long time;
    // we don't actually delete messages, but we mark them as not enabled to prevent the client from receiving them
    private Boolean deleted;
}
