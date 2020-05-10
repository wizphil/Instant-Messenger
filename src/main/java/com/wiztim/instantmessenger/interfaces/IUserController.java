package com.wiztim.instantmessenger.interfaces;

import com.wiztim.instantmessenger.dto.UserDTO;
import com.wiztim.instantmessenger.dto.UserProfileDTO;
import com.wiztim.instantmessenger.persistence.user.UserInfo;
import com.wiztim.instantmessenger.enums.Status;
import com.wiztim.instantmessenger.persistence.user.User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collection;
import java.util.UUID;

public interface IUserController {
    User createUser(@RequestBody UserInfo userInfo);

    void updateUserProfile(@RequestBody UserProfileDTO userProfileDTO);

    User getUser(@PathVariable("id") UUID id);

    User getUserByUsername(@PathVariable("username") String username);

    Collection<UserDTO> getAllUserInfo();

    UserDTO getUserInfo(@PathVariable("id") UUID id);

    UserDTO getUserInfoByUsername(@PathVariable("username") String username);

    void setUserStatus(@PathVariable("id") UUID id, @PathVariable("status") Status status);

    void setUserEnabled(@PathVariable("id") UUID id, @PathVariable("enabled") boolean enabled);

    // TODO add endpoint for setting window open/closed
}
