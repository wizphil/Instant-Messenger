package com.wiztim.instantmessenger.repository;

import com.wiztim.instantmessenger.exceptions.DuplicateEntityException;
import com.wiztim.instantmessenger.exceptions.InvalidEntityException;
import com.wiztim.instantmessenger.persistence.Group;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class GroupRepository {
    // TODO add  a database

    private final HashMap<UUID, Group> groupsCache = new HashMap<>();

    public Group get(UUID id) {
        return groupsCache.get(id);
    }

    public Set<Group> getEnabled(UUID userId) {
        return groupsCache.values()
                .stream()
                .filter(group -> group.getUserIds().contains(userId) && group.isEnabled())
                .collect(Collectors.toSet());
    }

    public void update(Group group) {
        if (exists(group)) {
            groupsCache.put(group.getId(), group);
        }

    }

    public void create(Group group) {
        if (group == null || group.getId() == null || group.getUserIds() == null || group.getUserIds().isEmpty()) {
            throw new InvalidEntityException();
        }

        if (exists(group)) {
            throw new DuplicateEntityException(group.getId());
        }

        groupsCache.put(group.getId(), group);
    }

    public boolean exists(Group group) {
        if (group == null) {
            return false;
        }

        return exists(group.getId());
    }

    public boolean exists(UUID id) {
        return groupsCache.containsKey(id);
    }
}
