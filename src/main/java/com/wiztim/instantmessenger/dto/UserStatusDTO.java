package com.wiztim.instantmessenger.dto;

import com.wiztim.instantmessenger.persistence.user.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusDTO {
    private UUID id;
    private UserStatus userStatus;
}
