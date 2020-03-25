package com.wiztim.instantmessenger.dto;

import com.wiztim.instantmessenger.persistence.user.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDTO {
    private UUID id;
    private UserInfo userInfo;
}
