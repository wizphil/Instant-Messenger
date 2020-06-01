package com.wizphil.instantmessenger.controllers;

import com.wizphil.instantmessenger.dto.UserInfoDTO;
import com.wizphil.instantmessenger.persistence.user.UserDetails;
import com.wizphil.instantmessenger.enums.Status;
import com.wizphil.instantmessenger.interfaces.IUserController;
import com.wizphil.instantmessenger.persistence.user.User;
import com.wizphil.instantmessenger.service.UserService;
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

@RestController
@RequestMapping("/v1/user")
@Slf4j
public class UserController implements IUserController {
    @Autowired
    private UserService userService;

    @Override
    @PostMapping
    public User createUser(@RequestBody UserDetails userDetails) {
        return userService.createUser(userDetails);
    }

    @Override
    @PostMapping("/username/{username}")
    public User createUserByUsername(@PathVariable("username") String username) {
        return userService.createUser(username);
    }

    @Override
    @PutMapping
    public void updateUserProfile(@RequestBody User user) {
        userService.updateUserProfile(user);
    }

    @Override
    @GetMapping("/{id}")
    public User getUser(@PathVariable("id") String id) {
        return userService.getExistingUser(id);
    }

    @Override
    @GetMapping("/{id}/fullname")
    public String getFullname(@PathVariable("id") String id) {
        return userService.getFullname(id);
    }

    @Override
    @GetMapping("/username/{username}")
    public User getUserByUsername(@PathVariable("username") String username) {
        return userService.getUserByUsername(username);
    }

    @Override
    @GetMapping("/info")
    public Collection<UserInfoDTO> getAllUserInfo() {
        return userService.getAllUserInfo();
    }

    @Override
    @GetMapping("/info/{id}")
    public UserInfoDTO getUserInfo(@PathVariable("id") String id) {
        return userService.getUserInfo(id);
    }

    @Override
    @GetMapping("/info/username/{username}")
    public UserInfoDTO getUserInfoByUsername(@PathVariable("username") String username) {
        return userService.getUserInfoByUsername(username);
    }

    @Override
    @PutMapping("/{id}/session/{sessionId}/status/{status}")
    public void setUserStatus(@PathVariable("id") String id, @PathVariable("sessionId") String sessionId, @PathVariable("status") Status status) {
        userService.setStatus(id, sessionId, status);
    }

    @Override
    @PutMapping("/{id}/enabled/{enabled}")
    public void setUserEnabled(@PathVariable("id") String id, @PathVariable("enabled") boolean enabled) {
        if (enabled) {
            userService.enableUser(id);
        } else {
            userService.disableUser(id);
        }
    }
}
