package com.wiztim.instantmessenger.persistence.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.websocket.Session;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {
    Session session;
    UserStatus userStatus;
}
