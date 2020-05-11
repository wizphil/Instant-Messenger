package com.wiztim.instantmessenger.persistence.user;

import com.wiztim.instantmessenger.dto.UserStatusDTO;
import com.wiztim.instantmessenger.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
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
                .status(Status.Offline)
                .time(Instant.now().toEpochMilli())
                .build();
    }

    public static UserStatusDTO toUserStatusDTO(UUID id, UserStatus userStatus) {
        return UserStatusDTO.builder()
                .id(id)
                .userStatus(userStatus)
                .build();
    }
}
