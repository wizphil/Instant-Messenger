package com.wizphil.instantmessenger.persistence.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@Builder
@Data
@Document
public class UserDetails {
    @Indexed
    private String username;
    private String fullname;
    private String extension;
    private boolean enabled;
}
