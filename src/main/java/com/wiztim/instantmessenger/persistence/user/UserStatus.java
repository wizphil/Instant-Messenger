package com.wiztim.instantmessenger.persistence.user;

import com.wiztim.instantmessenger.dto.UserStatusDTO;
import com.wiztim.instantmessenger.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatus {
    private Status status;
    private long time;

    public static UserStatus offlineNow() {
        return UserStatus.builder()
                .status(Status.OFFLINE)
                .time(new Date().getTime())
                .build();
    }

    public static UserStatusDTO toUserStatusDTO(UUID id, UserStatus userStatus) {
        return UserStatusDTO.builder()
                .id(id)
                .userStatus(userStatus)
                .build();
    }
}
