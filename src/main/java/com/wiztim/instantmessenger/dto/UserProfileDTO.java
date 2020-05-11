package com.wiztim.instantmessenger.dto;

import com.wiztim.instantmessenger.persistence.user.UserDetails;
import com.wiztim.instantmessenger.persistence.user.UserSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private UUID id;
    private UserDetails userDetails;
    private UserSettings userSettings;
    private boolean enabled;
}
