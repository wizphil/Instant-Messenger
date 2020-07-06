package com.wizphil.instantmessenger.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.wizphil.instantmessenger.exceptions.DuplicateEntityException;
import com.wizphil.instantmessenger.exceptions.InvalidEntityException;
import com.wizphil.instantmessenger.exceptions.RepositoryException;
import com.wizphil.instantmessenger.exceptions.UserNotFoundException;
import com.wizphil.instantmessenger.persistence.user.User;
import com.wizphil.instantmessenger.repository.UserRepository;
import com.wizphil.instantmessenger.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class UserCache {
    @Autowired
    private UserRepository userRepository;

    // keep a cache of the users because the service is read heavy
    private final LoadingCache<String, User> userCache = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public User load(String id) {
            return userRepository.findById(id).orElse(null);
        }
    });

    private final LoadingCache<String, String> usernameToId = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public String load(String username) {
            User user = userRepository.findFirstByUserDetails_Username(username);
            return user == null ? null : user.getId();
        }
    });

    public List<User> loadAll() {
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            userCache.put(user.getId(), user);
            usernameToId.put(user.getUserDetails().getUsername(), user.getId());
        }

        return allUsers;
    }

    public User get(String id) {
        try {
            return userCache.getUnchecked(id);
        } catch (CacheLoader.InvalidCacheLoadException e) {
            log.warn("get failed to find user {}", id);
            return null;
        }
    }

    public User getByUsername(String username) {
        String id;
        try {
            id = usernameToId.getUnchecked(username);
        } catch (CacheLoader.InvalidCacheLoadException e) {
            log.warn("getByUsername failed to find user for username {}", username);
            return null;
        }

        return get(id);
    }

    public User updateUser(User user) {
        log.info("updateUser started {}", user);
        UserService.validateUser(user);

        User oldUser = get(user.getId());
        if (oldUser == null) {
            log.error("updateUser called but user does not exist userId {}", user.getId());
            throw new UserNotFoundException(user.getId());
        }

        log.info("updateUser passed validation; new user {} old user {}", user, oldUser);

        try {
            user = userRepository.save(user);
        } catch (Exception e) {
            log.error("updateUser failed to save user to database {}", user, e);
            throw new RepositoryException(e);
        }

        userCache.put(user.getId(), user);

        // no null checking here, we assume service validates all properties are valid
        String oldUsername = oldUser.getUserDetails().getUsername();
        String newUsername = user.getUserDetails().getUsername();

        // update username cache if username changes
        if (!oldUsername.equals(newUsername)) {
            usernameToId.invalidate(oldUsername);
            usernameToId.put(newUsername, user.getId());
        }

        log.info("updateUser finished; new user {} old user {}", user, oldUser);
        return user;
    }

    public boolean isExistingUsername(String username) {

        try {
            usernameToId.getUnchecked(username);
            return true;
        } catch (CacheLoader.InvalidCacheLoadException e) {
            log.warn("getByUsername failed to find user for username {}", username);
            return false;
        }
    }

    public User createUser(User user) {
        log.info("createUser started {}", user);
        UserService.validateUser(user);

        if (user.getId() != null) {
            log.warn("createUser failed, attempted to create new user {}, but already has id {}", user, user.getId());
            throw new InvalidEntityException();
        }

        String username = user.getUserDetails().getUsername();
        if (isExistingUsername(username)) {
            log.warn("createUser failed, attempted to create new user {}, but already exists a user with username {}", user, username);
            throw new DuplicateEntityException(user.getId());
        }

        try {
            user = userRepository.insert(user);
        } catch (Exception e) {
            log.error("updateUser failed to insert user to database {}", user, e);
            throw new RepositoryException(e);
        }

        userCache.put(user.getId(), user);
        usernameToId.put(username, user.getId());

        log.info("createUser finished {}", user);
        return user;
    }

    public void clearAll() {
        userCache.invalidateAll();
        usernameToId.invalidateAll();
    }
}
