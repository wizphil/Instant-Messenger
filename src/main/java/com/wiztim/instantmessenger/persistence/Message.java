package com.wiztim.instantmessenger.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    @NonNull private UUID id;
    @NonNull private UUID from;
    @NonNull private UUID to;
    @NonNull private String content;
    private long time;
    // we don't actually delete messages, but we mark them as not enabled to prevent the client from receiving them
    private boolean enabled;
}
