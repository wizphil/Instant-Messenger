package com.wiztim.instantmessenger.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Set;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Group {
    private UUID id;
    private Set<UUID> userIds;
    private String groupName;
    private boolean enabled;
}
