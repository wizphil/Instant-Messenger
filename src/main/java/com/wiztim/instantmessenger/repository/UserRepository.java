package com.wiztim.instantmessenger.repository;

import com.wiztim.instantmessenger.exceptions.DuplicateEntityException;
import com.wiztim.instantmessenger.exceptions.InvalidEntityException;
import com.wiztim.instantmessenger.persistence.user.User;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.UUID;

@Component
public class UserRepository {
    // TODO add  a database

    // keep a cache of the users because the service is read heavy
    private final HashMap<UUID, User> userCache = new HashMap<>();

    public User get(UUID id) {
        // TODO if missing from cache, fetch from db... once it's added
        return userCache.get(id);
    }

    public void updateUser(User user) {
        if (userExists(user)) {
            // TODO update db first
            userCache.put(user.getId(), user);
        }
    }

    public boolean isExistingUsername(String username) {
        return userCache.values()
                .stream()
                .anyMatch(user -> user.getUserInfo().getUsername().equals(username));
    }

    public void createUser(User user) {
        if (user == null || user.getId() == null) {
            throw new InvalidEntityException();
        }

        if (userCache.containsKey(user.getId())) {
            throw new DuplicateEntityException(user.getId());
        }

        // TODO write to db

        userCache.put(user.getId(), user);
    }

    public boolean userExists(User user) {
        if (user == null) {
            return false;
        }

        return userExists(user.getId());
    }

    public boolean userExists(UUID id) {
        // TODO add a database lookup if cache is missing
        return userCache.containsKey(id);
    }
}
