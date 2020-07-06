package com.wizphil.instantmessenger.interfaces;

import com.wizphil.instantmessenger.dto.UserInfoDTO;
import com.wizphil.instantmessenger.persistence.user.UserDetails;
import com.wizphil.instantmessenger.enums.Status;
import com.wizphil.instantmessenger.persistence.user.User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collection;

public interface IUserController {
    User createUser(@RequestBody UserDetails userDetails);

    User createUserByUsername(@PathVariable("username") String username);

    void updateUserProfile(@RequestBody User user);

    User getUser(@PathVariable("id") String id);

    String getFullname(@PathVariable("id") String id);

    User getUserByUsername(@PathVariable("username") String username);

    Collection<UserInfoDTO> getAllUserInfo();

    UserInfoDTO getUserInfo(@PathVariable("id") String id);

    UserInfoDTO getUserInfoByUsername(@PathVariable("username") String username);

    void setUserStatus(@PathVariable("id") String id, @PathVariable("sessionId") String sessionId, @PathVariable("status") Status status);

    void setUserEnabled(@PathVariable("id") String id, @PathVariable("enabled") boolean enabled);

    // TODO add endpoint for setting window open/closed
}
