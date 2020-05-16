package com.wiztim.instantmessenger.controllers;

import com.wiztim.instantmessenger.dto.GroupUsersDTO;
import com.wiztim.instantmessenger.interfaces.IGroupController;
import com.wiztim.instantmessenger.persistence.Group;
import com.wiztim.instantmessenger.service.GroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/v1/group")
@Slf4j
public class GroupController implements IGroupController {
    @Autowired
    private GroupService groupService;

    @Override
    @PostMapping
    public Group createGroup(@RequestBody Group group) {
        return groupService.createGroup(group);
    }

    @Override
    @GetMapping("/{id}")
    public Group getGroup(@PathVariable("id") UUID id) {
        return groupService.getGroup(id);
    }

    @Override
    @GetMapping("/user/{userId}")
    public Set<Group> getGroupsForUser(@PathVariable("userId") UUID userId) {
        return groupService.getGroupsForUser(userId);
    }

    @Override
    @PutMapping("/{id}/user/{userId}")
    public void addUserToGroup(@PathVariable("id") UUID id, @PathVariable("userId") UUID userId) {
        groupService.addUsers(id, Set.of(userId));
    }

    @Override
    @PutMapping
    public void addUsersToGroup(@RequestBody GroupUsersDTO groupUsersDTO) {
        groupService.addUsers(groupUsersDTO.getGroupId(), groupUsersDTO.getUserIds());
    }

    @Override
    @DeleteMapping("/{id}/user/{userId}")
    public void removeUserFromGroup(@PathVariable("id") UUID id, @PathVariable("userId") UUID userId) {
        groupService.removeUser(id, userId);
    }
}
