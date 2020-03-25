package com.wiztim.instantmessenger.service;

import com.wiztim.instantmessenger.dto.UserDTO;
import com.wiztim.instantmessenger.dto.UserUpdateDTO;
import com.wiztim.instantmessenger.exceptions.UserNotFoundException;
import com.wiztim.instantmessenger.persistence.user.UserInfo;
import com.wiztim.instantmessenger.dto.UserStatusDTO;
import com.wiztim.instantmessenger.enums.Status;
import com.wiztim.instantmessenger.exceptions.DuplicateEntityException;
import com.wiztim.instantmessenger.exceptions.DisabledEntityException;
import com.wiztim.instantmessenger.exceptions.InvalidEntityException;
import com.wiztim.instantmessenger.exceptions.RepositoryException;
import com.wiztim.instantmessenger.exceptions.UserAlreadyEnabledException;
import com.wiztim.instantmessenger.exceptions.UserAlreadyDisabledException;
import com.wiztim.instantmessenger.persistence.user.User;
import com.wiztim.instantmessenger.persistence.user.UserSettings;
import com.wiztim.instantmessenger.persistence.user.UserStatus;
import com.wiztim.instantmessenger.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RabbitMQService rabbitMQService;

    private final Map<UUID, UserDTO> userDtoCache = new HashMap<>();

    public User createUser(UserInfo userInfo) {
        validateUserInfo(userInfo);
        if (userInfo.getFullName() == null) {
            userInfo.setFullName(userInfo.getUsername());
        }

        // check if user with username already exists
        if (userRepository.isExistingUsername(userInfo.getUsername())) {
            throw new DuplicateEntityException(userInfo.getUsername());
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .userInfo(userInfo)
                .userSettings(UserSettings.defaultSettings())
                .enabled(true)
                .build();

        // surely this can't happen
        if (userRepository.userExists(user.getId())) {
            throw new DuplicateEntityException(user.getId());
        }

        try {
            userRepository.createUser(user);
        } catch (Exception e) {
            throw new RepositoryException(e);
        }

        // update the cache, we have a new user!
        userDtoCache.put(user.getId(), User.toUserDTO(user, UserStatus.offlineNow()));

        // let clients know a new user has been made
        rabbitMQService.publishUser(user);
        return user;
    }

    public User getUser(UUID id) {
        return userRepository.get(id);
    }

    public User getUserByUsername(String username) {
        Optional<UUID> id = userDtoCache.values().stream().filter(userDto -> userDto.getUserInfo().getUsername().equals(username)).map(UserDTO::getId).findFirst();
        if (id.isEmpty()) {
            return null;
        }

        return getUser(id.get());
    }

    // When a user logs in, they need the entire list of enabled users with their status
    // We trust that the cache is accurate and up to date
    public Collection<UserDTO> getUserDTOs() {
        return userDtoCache.values();
    }

    public UserDTO getUserDTO(String username) {
        Optional<UserDTO> userDTO = userDtoCache.values().stream().filter(userDto -> userDto.getUserInfo().getUsername().equals(username)).findFirst();
        if (userDTO.isEmpty()) {
            return null;
        }

        return userDTO.get();
    }

    public UserDTO getUserDTO(UUID id) {
        return userDtoCache.get(id);
    }

    public void updateUser(UserUpdateDTO userUpdateDTO) {
        UserInfo userInfo = userUpdateDTO.getUserInfo();
        UserSettings userSettings = userUpdateDTO.getUserSettings();

        validateUserExists(userUpdateDTO.getId());
        validateUserInfo(userInfo);
        validateUserSettings(userSettings);

        User user = fetchUser(userUpdateDTO.getId());
        boolean userInfoChanged = !userInfo.equals(user.getUserInfo());

        user.setUserInfo(userInfo);
        user.setUserSettings(userSettings);

        updateUser(user);

        // let everyone know that someone's user info has changed
        if (userInfoChanged) {
            rabbitMQService.publishUserInfo(userInfo);
        }
    }

    public void updateUserStatus(UUID id, Status status) {
        // validate request
        validateEnabledUser(id);
        if (status == null) {
            throw new InvalidEntityException();
        }

        // send the status update
        UserStatus userStatus = UserStatus.builder()
                .status(status)
                .time(new Date().getTime())
                .build();

        UserStatusDTO userStatusDTO = UserStatus.toUserStatusDTO(id, userStatus);
        rabbitMQService.publishUserStatus(userStatusDTO);

        // update the userCache with new status
        UserDTO userDTO = userDtoCache.get(id);
        if (userDTO == null) {
            User user = fetchUser(id);
            userDTO = User.toUserDTO(user, userStatus);
        } else {
            userDTO.setUserStatus(userStatus);
        }

        userDtoCache.put(id, userDTO);
    }

    public void enableUser(UUID id) {
        User user = validateAndGetDisabledUser(id);
        user.setEnabled(true);
        updateUser(user);
        rabbitMQService.publishUser(user);
        // remove them from the userCache

    }

    public void disableUser(UUID id) {
        User user = validateAndGetEnabledUser(id);
        if (!user.isEnabled()) {
            throw new UserAlreadyDisabledException(id);
        }

        user.setEnabled(false);
        updateUser(user);
        rabbitMQService.publishUserDisabled(id);
    }

    // takes a list of userIds and returns which of those users are currently online
    protected List<UUID> getOnlineUserIds(List<UUID> userIds) {
        return userDtoCache.values()
                .stream()
                .filter(user -> !Status.OFFLINE.equals(user.getUserStatus().getStatus()))
                .map(UserDTO::getId)
                .collect(Collectors.toList());
    }

    // takes a list of userIds and returns which of those users are currently enabled
    protected List<UUID> getEnabled(List<UUID> userIds) {
        return userDtoCache.values()
                .stream()
                .filter(UserDTO::isEnabled)
                .map(UserDTO::getId)
                .collect(Collectors.toList());
    }

    public boolean isUserOffline(UUID id) {
        if (id == null || !userDtoCache.containsKey(id)) {
            return true;
        }

        return Status.OFFLINE.equals(userDtoCache.get(id).getUserStatus().getStatus());
    }

    protected void validateUserExists(UUID id) {
        if (id == null || !userRepository.userExists(id)) {
            throw new InvalidEntityException();
        }
    }

    protected void validateEnabledUser(UUID id) {
        validateUserExists(id);
        User user = userRepository.get(id);
        if (!user.isEnabled()) {
            throw new DisabledEntityException(id);
        }
    }

    protected User validateAndGetEnabledUser(UUID id) {
        validateUserExists(id);
        User user = userRepository.get(id);
        if (!user.isEnabled()) {
            throw new DisabledEntityException(id);
        }

        return user;
    }

    protected User validateAndGetDisabledUser(UUID id) {
        validateUserExists(id);
        User user = userRepository.get(id);
        if (user.isEnabled()) {
            throw new UserAlreadyEnabledException(id);
        }

        return user;
    }

    private void updateUser(User user) {
        try {
            userRepository.updateUser(user);
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    private void validateUserInfo(UserInfo userInfo) {
        if (userInfo == null || userInfo.getUsername() == null || userInfo.getUsername().isBlank()) {
            log.error("User info failed validation: " + userInfo);
            throw new InvalidEntityException();
        }
    }

    private void validateUserSettings(UserSettings userSettings) {
        if (userSettings == null || userSettings.getFontSize() <= 0 || userSettings.getFontSize() > 100) {
            log.error("User settings failed validation: " + userSettings);
            throw new InvalidEntityException();
        }
    }

    private User fetchUser(UUID id) {
        User user;
        try {
            user = userRepository.get(id);
        } catch (Exception e) {
            log.error("Failed to get user: " + id + " from the db.", e);
            throw new RepositoryException(e);
        }

        if (user == null) {
            throw new UserNotFoundException(id);
        }

        return user;
    }
}
