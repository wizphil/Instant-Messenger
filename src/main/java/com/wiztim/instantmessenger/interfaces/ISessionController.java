package com.wiztim.instantmessenger.interfaces;

import com.wiztim.instantmessenger.dto.UserProfileDTO;
import com.wiztim.instantmessenger.enums.Status;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

public interface ISessionController {
    UserProfileDTO login(@PathVariable("username") String username, @PathVariable("connection") String connection);

    boolean reconnect(@PathVariable("id") UUID id, @PathVariable("connection") String connection, @PathVariable("status") Status status);

    void logout(@PathVariable("id") UUID id, @PathVariable("connection") String connection);
}
