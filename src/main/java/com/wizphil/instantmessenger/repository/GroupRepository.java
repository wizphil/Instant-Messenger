package com.wizphil.instantmessenger.repository;

import com.wizphil.instantmessenger.persistence.Group;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GroupRepository extends MongoRepository<Group, String> {

}
