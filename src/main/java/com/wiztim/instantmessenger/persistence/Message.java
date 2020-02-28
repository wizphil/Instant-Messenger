package com.wiztim.instantmessenger.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class Message {
    private UUID id;
}
