package com.wiztim.instantmessenger.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OperationCode {
    DirectMessage("dm"),
    UserTypingPing("ping"),
    GroupTypingPing("gping"),
    NewGroup("newgroup"),
    UserRemovedFromGroup("rmuser"),
    NewUser("newuser"),
    UpdateUserInfo("uinfo"),
    UpdateUserStatus("ustatus"),
    DisableUser("deluser");

    private String opcode;
}
