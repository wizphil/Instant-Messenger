package com.wiztim.instantmessenger.persistence.user;

import com.wiztim.instantmessenger.dto.UserDTO;
import com.wiztim.instantmessenger.dto.UserInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private UUID id;
    private UserInfo userInfo;
    private UserSettings userSettings;
    private boolean enabled;

    public static UserDTO toUserDTO(User user, UserStatus userStatus) {
        return UserDTO.builder()
                .id(user.getId())
                .userInfo(user.getUserInfo())
                .userStatus(userStatus)
                .enabled(user.isEnabled())
                .build();
    }

    public static UserInfoDTO toUserInfoDTO(User user) {
        return UserInfoDTO.builder()
                .id(user.getId())
                .userInfo(user.getUserInfo())
                .build();
    }
}
