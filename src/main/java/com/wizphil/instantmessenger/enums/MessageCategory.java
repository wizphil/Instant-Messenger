package com.wizphil.instantmessenger.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageCategory {
    DirectMessage("dm"),
    UserTypingPing("ping"),
    GroupTypingPing("gping"),
    NewGroup("newgroup"),
    UsersAddedToGroup("addgusers"),
    UserRemovedFromGroup("rmguser"),
    NewUser("newuser"),
    UpdateUserDetails("udetails"),
    UpdateUserStatus("ustatus"),
    DisableUser("deluser"),
    EstablisedSession("newsession"),
    CloseSession("delsession");

    private String opcode;
}
