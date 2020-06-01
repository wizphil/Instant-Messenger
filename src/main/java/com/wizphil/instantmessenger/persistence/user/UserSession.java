package com.wizphil.instantmessenger.persistence.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import javax.websocket.Session;

@AllArgsConstructor
@Builder
@Data
public class UserSession {
    Session session;
    UserStatus userStatus;
}
