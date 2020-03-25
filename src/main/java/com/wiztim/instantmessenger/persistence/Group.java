package com.wiztim.instantmessenger.persistence;

import com.wiztim.instantmessenger.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Group {
    private UUID id;
    private List<UUID> userIds;
    private boolean enabled;
}
