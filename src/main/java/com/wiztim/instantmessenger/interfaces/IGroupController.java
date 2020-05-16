package com.wiztim.instantmessenger.interfaces;

import com.wiztim.instantmessenger.dto.GroupUsersDTO;
import com.wiztim.instantmessenger.persistence.Group;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Set;
import java.util.UUID;

public interface IGroupController {
    Group createGroup(@RequestBody Group group);

    Group getGroup(@PathVariable("id") UUID id);

    Set<Group> getGroupsForUser(@PathVariable("userId") UUID userId);

    void addUserToGroup(@PathVariable("id") UUID id, @PathVariable("userId") UUID userId);

    void addUsersToGroup(@RequestBody GroupUsersDTO groupUsersDTO);

    void removeUserFromGroup(@PathVariable("id") UUID id, @PathVariable("userId") UUID userId);
}
