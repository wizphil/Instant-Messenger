package com.wizphil.instantmessenger.dto;

import com.wizphil.instantmessenger.persistence.user.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusDTO {
    private String id;
    private UserStatus userStatus;
}
