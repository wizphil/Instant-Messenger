package com.wizphil.instantmessenger.persistence.user;

import com.wizphil.instantmessenger.dto.UserInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

@AllArgsConstructor
@Builder
@Data
public class User {
    @Id
    private String id;
    private UserDetails userDetails;
    private UserSettings userSettings;

    public static UserInfoDTO toUserInfoDTO(User user, UserStatus userStatus) {
        return UserInfoDTO.builder()
                .id(user.getId())
                .userDetails(user.getUserDetails())
                .userStatus(userStatus)
                .build();
    }

    public static UserInfoDTO toUserInfoDTO(User user) {
        return toUserInfoDTO(user, UserStatus.offlineNow());
    }
}
