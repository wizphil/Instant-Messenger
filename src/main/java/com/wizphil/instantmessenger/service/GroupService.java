package com.wizphil.instantmessenger.service;

import com.google.common.collect.Sets;
import com.wizphil.instantmessenger.dto.GroupUserDTO;
import com.wizphil.instantmessenger.dto.GroupUsersDTO;
import com.wizphil.instantmessenger.dto.MessageWrapperDTO;
import com.wizphil.instantmessenger.enums.MessageCategory;
import com.wizphil.instantmessenger.exceptions.GroupNotFoundException;
import com.wizphil.instantmessenger.exceptions.InvalidEntityException;
import com.wizphil.instantmessenger.exceptions.NullIdException;
import com.wizphil.instantmessenger.exceptions.UserNotInGroupException;
import com.wizphil.instantmessenger.persistence.Group;
import com.wizphil.instantmessenger.cache.GroupCache;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Setter
@Slf4j
public class GroupService {

    @Autowired
    private GroupCache groupCache;

    @Autowired
    private UserService userService;

    @Autowired
    private SessionService sessionService;

    public Group createGroup(Group group) {
        log.debug("Creating group=" + group);
        if (group == null || group.getUserIds() == null || group.getUserIds().size() <= 1) {
            log.warn("Error creating group, missing required fields. group=" + group);
            throw new InvalidEntityException();
        }

        Set<String> userIds = userService.getEnabledUserIds(group.getUserIds());
        if (group.getUserIds().size() <= 1) {
            log.warn("Error creating group, not enough enabled users. id=" + group.getId());
            throw new InvalidEntityException();
        }

        if (userIds.size() < group.getUserIds().size()) {
            log.warn("Create group was called with deactivated users. Enabled Users: {} Original Users: {}", userIds, group.getUserIds());
        }

        group.setUserIds(userIds);
        group.setEnabled(true);

        groupCache.create(group);

        sessionService.sendMessageToUsers(group.getUserIds(), new MessageWrapperDTO(MessageCategory.NewGroup, group));

        log.debug("Group created! id=" + group.getId());
        return group;
    }

    public Group getGroup(String id) {
        return groupCache.get(id);
    }

    public Set<Group> getGroupsForUser(String userId) {
        if (userId == null ) {
            throw new NullIdException();
        }

        return groupCache.getGroupsForUser(userId);
    }

    public void addUsers(String groupId, Set<String> userIds) {
        log.debug("Attempting to add userIds {} to group {}", userIds, groupId);
        if (groupId == null || userIds == null || userIds.size() == 0) {
            log.warn("addUser called with null group or user id, groupId {} userIds {}", groupId, userIds);
            throw new NullIdException();
        }

        Group group = getExistingGroup(groupId);

        userIds.removeAll(group.getUserIds());
        Set<String> enabledNotInGroupUserIds = userService.getEnabledUserIds(group.getUserIds());

        if (enabledNotInGroupUserIds.size() < userIds.size()) {
            log.warn("addUsers called with user(s) either already in group or deactivated, groupId {} userIds {} enabledNotInGroupUserIds {}", groupId, userIds, enabledNotInGroupUserIds);
        }

        group.setUserIds(Sets.union(group.getUserIds(), enabledNotInGroupUserIds));
        groupCache.update(group);

        GroupUsersDTO groupUsersDTO = GroupUsersDTO.builder()
                .groupId(group.getId())
                .userIds(enabledNotInGroupUserIds)
                .build();

        sessionService.sendMessageToUsers(group.getUserIds(), new MessageWrapperDTO(MessageCategory.UsersAddedToGroup, groupUsersDTO));
    }

    public void removeUser(String groupId, String userId) {
        log.debug("Attempting to remove user=" + userId + " from group=" + groupId);
        if (groupId == null || userId == null) {
            throw new NullIdException();
        }

        Group group = getExistingGroup(groupId);
        if (!group.getUserIds().contains(userId)) {
            throw new UserNotInGroupException(groupId, userId);
        }

        group.getUserIds().remove(userId);
        if (group.getUserIds().isEmpty()) {
            group.setEnabled(false);
        }

        groupCache.update(group);

        GroupUserDTO groupUserDTO = GroupUserDTO.builder()
                .groupId(group.getId())
                .userId(userId)
                .build();

        // send a message to the removed user and to the remaining users
        MessageWrapperDTO removedGroupUserMessage = new MessageWrapperDTO(MessageCategory.UserRemovedFromGroup, groupUserDTO);
        sessionService.sendMessageToUser(userId, removedGroupUserMessage);
        sessionService.sendMessageToUsers(group.getUserIds(), removedGroupUserMessage);
    }

    protected Group getExistingGroup(String id) {
        if (id == null) {
            throw new NullIdException();
        }

        Group group = groupCache.get(id);
        if (group == null) {
            throw new GroupNotFoundException(id);
        }

        return group;
    }
}
