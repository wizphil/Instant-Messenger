package com.wizphil.instantmessenger.interfaces;

import com.wizphil.instantmessenger.dto.GroupUsersDTO;
import com.wizphil.instantmessenger.persistence.Group;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Set;

public interface IGroupController {
    Group createGroup(@RequestBody Group group);

    Group getGroup(@PathVariable("id") String id);

    Set<Group> getGroupsForUser(@PathVariable("userId") String userId);

    void addUserToGroup(@PathVariable("id") String id, @PathVariable("userId") String userId);

    void addUsersToGroup(@RequestBody GroupUsersDTO groupUsersDTO);

    void removeUserFromGroup(@PathVariable("id") String id, @PathVariable("userId") String userId);
}
