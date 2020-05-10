package com.wiztim.instantmessenger.controllers;

import com.wiztim.instantmessenger.dto.UserDTO;
import com.wiztim.instantmessenger.dto.UserProfileDTO;
import com.wiztim.instantmessenger.persistence.user.UserInfo;
import com.wiztim.instantmessenger.enums.Status;
import com.wiztim.instantmessenger.interfaces.IUserController;
import com.wiztim.instantmessenger.persistence.user.User;
import com.wiztim.instantmessenger.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.UUID;

@RestController
@RequestMapping("/v1/user")
@Slf4j
public class UserController implements IUserController {
    @Autowired
    private UserService userService;

    @Override
    @PostMapping
    public User createUser(@RequestBody UserInfo userInfo) {
        return userService.createUser(userInfo);
    }

    @Override
    @PutMapping
    public void updateUserProfile(@RequestBody UserProfileDTO userProfileDTO) {
        userService.updateUserProfile(userProfileDTO);
    }

    @Override
    @GetMapping("/{id}")
    public User getUser(@PathVariable("id") UUID id) {
        return userService.getUser(id);
    }

    @Override
    @GetMapping("/username/{username}")
    public User getUserByUsername(@PathVariable("username") String username) {
        return userService.getUserByUsername(username);
    }

    @Override
    @GetMapping("/info")
    public Collection<UserDTO> getAllUserInfo() {
        return userService.getUserDTOs();
    }

    @Override
    @GetMapping("/info/{id}")
    public UserDTO getUserInfo(@PathVariable("id") UUID id) {
        return userService.getUserDTO(id);
    }

    @Override
    @GetMapping("/info/username/{username}")
    public UserDTO getUserInfoByUsername(@PathVariable("username") String username) {
        return userService.getUserDTO(username);
    }

    @Override
    @PutMapping("/{id}/status/{status}")
    public void setUserStatus(@PathVariable("id") UUID id, @PathVariable("status") Status status) {
        userService.updateUserStatus(id, status);
    }

    @Override
    @PutMapping("/{id}/enabled/{enabled}")
    public void setUserEnabled(@PathVariable("id") UUID id, @PathVariable("enabled") boolean enabled) {
        if (enabled) {
            userService.enableUser(id);
        } else {
            userService.disableUser(id);
        }
    }
}
