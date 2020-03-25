package com.wiztim.instantmessenger.dto;

import com.wiztim.instantmessenger.persistence.user.User;
import com.wiztim.instantmessenger.persistence.user.UserInfo;
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
public class UserDTO {
    private UUID id;
    private UserInfo userInfo;
    private UserStatus userStatus;
    private boolean enabled;
}
