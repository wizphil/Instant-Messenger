package com.wiztim.instantmessenger.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageCategory {
    DirectMessage("dm"),
    UserTypingPing("ping"),
    GroupTypingPing("gping"),
    NewGroup("newgroup"),
    UserRemovedFromGroup("rmuser"),
    NewUser("newuser"),
    UpdateUserDetails("udetails"),
    UpdateUserStatus("ustatus"),
    DisableUser("deluser");

    private String opcode;
}
