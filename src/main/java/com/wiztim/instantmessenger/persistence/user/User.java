package com.wiztim.instantmessenger.persistence.user;

import com.wiztim.instantmessenger.dto.UserInfoDTO;
import com.wiztim.instantmessenger.dto.UserDetailsDTO;
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
    private UserDetails userDetails;
    private UserSettings userSettings;

    public static UserInfoDTO toUserDTO(User user, UserStatus userStatus) {
        return UserInfoDTO.builder()
                .id(user.getId())
                .userDetails(user.getUserDetails())
                .userStatus(userStatus)
                .build();
    }

    public static UserInfoDTO toUserDTO(User user) {
        return toUserDTO(user, UserStatus.offlineNow());
    }

    public static UserDetailsDTO toUserInfoDTO(User user) {
        return UserDetailsDTO.builder()
                .id(user.getId())
                .userDetails(user.getUserDetails())
                .build();
    }
}
