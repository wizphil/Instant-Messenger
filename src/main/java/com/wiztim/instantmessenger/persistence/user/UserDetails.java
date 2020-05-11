package com.wiztim.instantmessenger.persistence.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetails {
    private String username;
    private String fullname;
    private String extension;
    private boolean enabled;
}
