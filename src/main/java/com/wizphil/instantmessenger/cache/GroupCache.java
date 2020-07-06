package com.wizphil.instantmessenger.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.wizphil.instantmessenger.exceptions.GroupNotFoundException;
import com.wizphil.instantmessenger.exceptions.InvalidEntityException;
import com.wizphil.instantmessenger.exceptions.NullIdException;
import com.wizphil.instantmessenger.exceptions.RepositoryException;
import com.wizphil.instantmessenger.persistence.Group;
import com.wizphil.instantmessenger.repository.GroupRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
public class GroupCache {
    @Autowired
    private GroupRepository groupRepository;

    private final SetMultimap<String, String> userIdToGroupIds = Multimaps.synchronizedSetMultimap(HashMultimap.create());
    private final LoadingCache<String, Group> groupCache = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public Group load(String id) {
            return groupRepository.findById(id).orElse(null);
        }
    });

    public Group get(String id) {
        try {
            return groupCache.getUnchecked(id);
        } catch (CacheLoader.InvalidCacheLoadException e) {
            return null;
        }
    }

    public Set<Group> getGroupsForUser(String userId) {
        return null;
    }

    public void update(Group group) {
        if (group == null) {
            log.error("updateGroup called with null");
            throw new NullIdException();
        }

        Group oldGroup = get(group.getId());
        if (oldGroup == null) {
            log.error("updateGroup could not find old group");
            throw new GroupNotFoundException(group.getId());
        }

        log.info("updateGroup started; new group {} old group {}", group, oldGroup);

        try {
            group = groupRepository.save(group);
        } catch (Exception e) {
            log.error("updateUser failed to save user to database {}", group, e);
            throw new RepositoryException(e);
        }

        groupCache.put(group.getId(), group);
        log.info("updateGroup finished; new group {} old group {}", group, oldGroup);
    }

    public void create(Group group) {
        if (group == null || group.getUserIds() == null || group.getUserIds().isEmpty()) {
            throw new InvalidEntityException();
        }

        group = groupRepository.insert(group);
        groupCache.put(group.getId(), group);
    }
}
