package com.wizphil.instantmessenger.repository;

import com.wizphil.instantmessenger.persistence.GroupMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface GroupMessageRepository extends MongoRepository<GroupMessage, String> {
    @Query(value = "{ groupId: ?0, time: { $lte: ?1 }, deleted: false }",
            sort = "{ time: -1 }",
            fields = "{_id : 1}")
    Page<GroupMessage> findConversationBeforeTime(String groupId, long afterTime, Pageable pageable);
}
