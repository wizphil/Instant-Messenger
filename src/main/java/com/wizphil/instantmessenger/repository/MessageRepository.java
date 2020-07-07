package com.wizphil.instantmessenger.repository;

import com.wizphil.instantmessenger.persistence.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface MessageRepository extends MongoRepository<Message, String> {
    @Query(value = "{ conversationId: ?0, time: { $lt: ?1 }, deleted: false }",
            sort = "{ time: -1 }",
            fields = "{_id : 1, to: 1, from: 1, content: 1, time: 1}")
    Page<Message> findConversationBeforeTime(String conversationId, long beforeTime, Pageable pageable);

    @Query(value = "{ conversationId: ?0, time: { $gte: ?1 }, deleted: false }",
            sort = "{ time: -1 }",
            fields = "{_id : 1}")
    Page<Message> findMessagesToUserAfterTime(String conversationId, long afterTime, Pageable pageable);
}
