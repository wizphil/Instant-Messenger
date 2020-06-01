package com.wizphil.instantmessenger.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@AllArgsConstructor
@Builder
@Data
@Document
public class Group {
    @Id
    private String id;
    private Set<String> userIds;
    private String groupName;
    private boolean enabled;
}
