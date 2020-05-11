package com.wiztim.instantmessenger.service;

import com.wiztim.instantmessenger.dto.GroupUserDTO;
import com.wiztim.instantmessenger.dto.MessageWrapperDTO;
import com.wiztim.instantmessenger.enums.MessageCategory;
import com.wiztim.instantmessenger.exceptions.DuplicateEntityException;
import com.wiztim.instantmessenger.exceptions.GroupNotFoundException;
import com.wiztim.instantmessenger.exceptions.InvalidEntityException;
import com.wiztim.instantmessenger.exceptions.NullIdException;
import com.wiztim.instantmessenger.exceptions.RepositoryException;
import com.wiztim.instantmessenger.exceptions.UserNotInGroupException;
import com.wiztim.instantmessenger.persistence.Group;
import com.wiztim.instantmessenger.repository.GroupRepository;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Setter
@Slf4j
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

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

        List<UUID> userIds = userService.getEnabled(group.getUserIds());
        if (group.getUserIds().size() <= 1) {
            log.warn("Error creating group, not enough enabled users. id=" + group.getId());
            throw new InvalidEntityException();
        }

        group.setId(UUID.randomUUID());
        group.setUserIds(userIds);
        group.setEnabled(true);

        // surely this can't happen
        if (groupRepository.groupExists(group.getId())) {
            log.warn("Error creating group, group already exists with id=" + group.getId());
            throw new DuplicateEntityException(group.getId());
        }

        try {
            groupRepository.createGroup(group);
        } catch (Exception e) {
            log.warn("Error creating group, repository write failed. id=" + group.getId(), e);
            throw new RepositoryException(e);
        }

        List<UUID> onlineUserIds = userService.getOnlineUserIds(group.getUserIds());

        sessionService.sendMessageToUsers(onlineUserIds, new MessageWrapperDTO(MessageCategory.NewGroup, group));

        log.debug("Group created! id=" + group.getId());
        return group;
    }

    public Group getGroup(UUID id) {
        return groupRepository.get(id);
    }

    public List<Group> getGroupsForUser(UUID userId) {
        if (userId == null ) {
            throw new NullIdException();
        }

        return groupRepository.getEnabled(userId);
    }

    public void removeUser(UUID groupId, UUID userId) {
        log.debug("Attempting to remove user=" + userId + " from group=" + groupId);
        if (groupId == null || userId == null) {
            throw new NullIdException();
        }

        Group group = validateAndGetGroup(groupId);
        if (!group.getUserIds().contains(userId)) {
            throw new UserNotInGroupException(groupId, userId);
        }

        // get the online users now, before we remove the user from the group
        List<UUID> onlineUserIds = userService.getOnlineUserIds(group.getUserIds());
        group.getUserIds().remove(userId);
        if (group.getUserIds().isEmpty()) {
            group.setEnabled(false);
        }

        try {
            groupRepository.updateGroup(group);
        } catch (Exception e) {
            throw new RepositoryException(e);
        }

        GroupUserDTO groupUserDTO = GroupUserDTO.builder()
                .groupId(group.getId())
                .userId(userId)
                .build();

        sessionService.sendMessageToUsers(onlineUserIds, new MessageWrapperDTO(MessageCategory.UserRemovedFromGroup, groupUserDTO));
    }

    protected Group validateAndGetGroup(UUID id) {
        if (id == null) {
            throw new NullIdException();
        }

        Group group = groupRepository.get(id);
        if (group == null) {
            throw new GroupNotFoundException(id);
        }

        return group;
    }
}
