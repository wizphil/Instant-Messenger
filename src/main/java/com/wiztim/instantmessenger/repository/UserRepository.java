package com.wiztim.instantmessenger.repository;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.wiztim.instantmessenger.exceptions.DuplicateEntityException;
import com.wiztim.instantmessenger.exceptions.InvalidEntityException;
import com.wiztim.instantmessenger.exceptions.NullIdException;
import com.wiztim.instantmessenger.exceptions.UserNotFoundException;
import com.wiztim.instantmessenger.persistence.user.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
@Slf4j
public class UserRepository {
    // TODO add  a database

    // keep a cache of the users because the service is read heavy
    private final LoadingCache<UUID, User> userCache = CacheBuilder.newBuilder().build(new CacheLoader<UUID, User>() {
        @Override
        public User load(UUID id) {
            // TODO if missing from cache, fetch from db... once it's added
            return null;
        }
    });

    private final LoadingCache<String, UUID> usernameToId = CacheBuilder.newBuilder().build(new CacheLoader<String, UUID>() {
        @Override
        public UUID load(String username) {
            // TODO if missing from cache, fetch from db... once it's added
            return null;
        }
    });

    public User get(UUID id) {
        return userCache.getUnchecked(id);
    }

    public User getByUsername(String username) {
        UUID id = usernameToId.getUnchecked(username);
        if (id == null) {
            log.warn("getByUsername failed to find user for username {}", username);
            return null;
        }

        return userCache.getUnchecked(id);
    }

    public void updateUser(User user) {
        if (user == null) {
            log.error("updateUser called with null");
            throw new NullIdException();
        }

        User oldUser = get(user.getId());
        if (oldUser == null) {
            log.error("updateUser called but user does not exist userId {}", user.getId());
            throw new UserNotFoundException(user.getId());
        }

        log.info("updateUser started; new user {} old user {}", user, oldUser);

        // TODO update db first
        userCache.put(user.getId(), user);

        // no null checking here, we assume service validates all properties are valid
        Set<String> oldUsernames = oldUser.getUserDetails().getUsernames();
        Set<String> newUsernames = user.getUserDetails().getUsernames();

        // remove unassociated usernames from the cache
        for (String username : oldUsernames) {
            if (!newUsernames.contains(username)) {
                usernameToId.invalidate(username);
            }
        }

        // add newly associated usernames to the cache
        for (String username : newUsernames) {
            if (!oldUsernames.contains(username)) {
                usernameToId.put(username, user.getId());
            }
        }

        log.info("updateUser finished; new user {} old user {}", user, oldUser);
    }

    public boolean isExistingUsername(String username) {
        return usernameToId.getUnchecked(username) != null;
    }

    public void createUser(User user) {
        log.info("createUser started {}", user);
        if (user == null || user.getId() == null || user.getUserDetails() == null || user.getUserDetails().getUsernames() == null || user.getUserDetails().getUsernames().size() == 0) {
            throw new InvalidEntityException();
        }

        if (userCache.getUnchecked(user.getId()) != null) {
            log.warn("createUser failed, attempted to create new user {}, but already exists with id {}", user, user.getId());
            throw new DuplicateEntityException(user.getId());
        }

        Set<String> usernames =  user.getUserDetails().getUsernames();
        for (String username : usernames) {
            if (isExistingUsername(username)) {
                log.warn("createUser failed, attempted to create new user {}, but already exists a user with username {}", user, username);
                throw new DuplicateEntityException(user.getId());
            }
        }

        // TODO write to db

        userCache.put(user.getId(), user);
        for (String username : usernames) {
            usernameToId.put(username, user.getId());
        }

        log.info("createUser finished {}", user);
    }
}
