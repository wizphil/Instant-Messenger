package com.wiztim.instantmessenger.interfaces;

import com.wiztim.instantmessenger.dto.UserInfoDTO;
import com.wiztim.instantmessenger.dto.UserProfileDTO;
import com.wiztim.instantmessenger.persistence.user.UserDetails;
import com.wiztim.instantmessenger.enums.Status;
import com.wiztim.instantmessenger.persistence.user.User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collection;
import java.util.UUID;

public interface IUserController {
    User createUser(@RequestBody UserDetails userDetails);

    void updateUserProfile(@RequestBody UserProfileDTO userProfileDTO);

    User getUser(@PathVariable("id") UUID id);

    User getUserByUsername(@PathVariable("username") String username);

    Collection<UserInfoDTO> getAllUserInfo();

    UserInfoDTO getUserInfo(@PathVariable("id") UUID id);

    UserInfoDTO getUserInfoByUsername(@PathVariable("username") String username);

    void setUserStatus(@PathVariable("id") UUID id, @PathVariable("sessionId") String sessionId, @PathVariable("status") Status status);

    void setUserEnabled(@PathVariable("id") UUID id, @PathVariable("enabled") boolean enabled);

    // TODO add endpoint for setting window open/closed
}
