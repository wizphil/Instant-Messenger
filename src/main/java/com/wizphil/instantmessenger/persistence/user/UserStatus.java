package com.wizphil.instantmessenger.persistence.user;

import com.wizphil.instantmessenger.dto.UserStatusDTO;
import com.wizphil.instantmessenger.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@AllArgsConstructor
@Builder
@Data
public class UserStatus {
    private Status status;
    private long time;

    public static UserStatus offlineNow() {
        return UserStatus.builder()
                .status(Status.Offline)
                .time(Instant.now().toEpochMilli())
                .build();
    }

    public static UserStatusDTO toUserStatusDTO(String id, UserStatus userStatus) {
        return UserStatusDTO.builder()
                .id(id)
                .userStatus(userStatus)
                .build();
    }
}
