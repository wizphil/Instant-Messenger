package com.wizphil.instantmessenger.repository;

import com.wizphil.instantmessenger.persistence.user.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
    User findFirstByUserDetails_Username(String username);
}
