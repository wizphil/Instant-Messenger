package com.wizphil.instantmessenger.service;

import com.wizphil.instantmessenger.dto.UserInfoDTO;
import com.wizphil.instantmessenger.persistence.user.User;
import com.wizphil.instantmessenger.persistence.user.UserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
public class UserServiceTests {
    @Autowired
    UserService service;

    @Test
    public void createsUser() {

        UserDetails userDetails = UserDetails.builder()
                .username("pkelly")
                .fullname("Phil Kelly")
                .extension("117")
                .enabled(true)
                .build();

        User phil = service.createUser(userDetails);
        assertThat(phil).isNotNull();
        assertThat(phil.getId()).isNotNull().isNotBlank();
        assertThat(phil.getUserDetails()).isNotNull();
        assertThat(phil.getUserSettings()).isNotNull();
        assertThat(phil.getUserDetails().getUsername()).isEqualTo(userDetails.getUsername());
        assertThat(phil.getUserDetails().getFullname()).isEqualTo(userDetails.getFullname());
        assertThat(phil.getUserDetails().isEnabled()).isTrue();
    }

    @Test
    public void getsUserByUsername() {

        UserDetails userDetails = UserDetails.builder()
                .username("kchen")
                .fullname("Kevin Chen")
                .extension("FB")
                .enabled(true)
                .build();

        User result = service.createUser(userDetails);

        User kevin = service.getExistingUser(result.getId());

        assertThat(kevin).isNotNull();
        assertThat(kevin.getId()).isNotNull().isNotBlank();
        assertThat(kevin.getUserDetails()).isNotNull();
        assertThat(kevin.getUserSettings()).isNotNull();
        assertThat(kevin.getUserDetails().getUsername()).isEqualTo(userDetails.getUsername());
        assertThat(kevin.getUserDetails().getFullname()).isEqualTo(userDetails.getFullname());
        assertThat(kevin.getUserDetails().isEnabled()).isTrue();
    }

    @Test
    public void getsAllUserInfo() {

        UserDetails brianUserDetails = UserDetails.builder()
                .username("bmcdowell")
                .fullname("Brian McDowell")
                .extension("ADT")
                .enabled(true)
                .build();

        UserDetails jeffUserDetails = UserDetails.builder()
                .username("jeffery seal")
                .fullname("Jeffery McDowell")
                .extension("50")
                .enabled(true)
                .build();

        User brian = service.createUser(brianUserDetails);
        User jeff = service.createUser(jeffUserDetails);

        boolean hasBrian = false;
        boolean hasJeff = false;
        Collection<UserInfoDTO> userInfos = service.getAllUserInfo();
        for (UserInfoDTO userInfo : userInfos) {
            if (userInfo.getId().equals(brian.getId())) {
                hasBrian = true;
            }

            if (userInfo.getId().equals(jeff.getId())) {
                hasJeff = true;
            }
        }

        assertThat(hasBrian).isTrue();
        assertThat(hasJeff).isTrue();
    }

    @Test
    public void getsUserInfoByUsername() {

        UserDetails phoenixUserDetails = UserDetails.builder()
                .username("phoenix")
                .fullname("Phoenix the cat")
                .extension("meow")
                .enabled(true)
                .build();

        User phoenix = service.createUser(phoenixUserDetails);

        UserInfoDTO userInfo = service.getUserInfoByUsername(phoenixUserDetails.getUsername());

        assertThat(userInfo).isNotNull();
        assertThat(userInfo.getId()).isEqualTo(phoenix.getId());
    }

    @Test
    public void getsUserInfo() {

        UserDetails phoenixUserDetails = UserDetails.builder()
                .username("phoenix2")
                .fullname("Phoenix the second cat")
                .extension("meow")
                .enabled(true)
                .build();

        User phoenix = service.createUser(phoenixUserDetails);

        UserInfoDTO userInfo = service.getUserInfo(phoenix.getId());

        assertThat(userInfo).isNotNull();
        assertThat(userInfo.getId()).isEqualTo(phoenix.getId());
    }

    @Test
    public void updatesUserProfile() {

        UserDetails phoenixUserDetails = UserDetails.builder()
                .username("phoenix3")
                .fullname("Phoenix the third cat")
                .extension("meow")
                .enabled(true)
                .build();

        User phoenix = service.createUser(phoenixUserDetails);

        phoenix.getUserDetails().setFullname("Phoenix Reborn");
        phoenix.getUserSettings().setFontSize(42);

        User phoenixReborn = service.updateUserProfile(phoenix);

        assertThat(phoenixReborn).isNotNull();
        assertThat(phoenixReborn.getId()).isEqualTo(phoenix.getId());
        assertThat(phoenixReborn.getUserDetails().getFullname()).isEqualTo("Phoenix Reborn");
        assertThat(phoenixReborn.getUserSettings().getFontSize()).isEqualTo(42);
    }

    @Test
    public void enablesUser() {

        UserDetails disabledUserDetails = UserDetails.builder()
                .username("disabled-now-enabled-later")
                .fullname("I was disabled, now I am enabled!")
                .extension("disabled")
                .enabled(false)
                .build();

        User disabledUser = service.createUser(disabledUserDetails);
        assertThat(disabledUser).isNotNull();
        assertThat(disabledUser.getUserDetails().isEnabled()).isFalse();

        User enabledUser = service.enableUser(disabledUser.getId());

        assertThat(enabledUser).isNotNull();
        assertThat(enabledUser.getId()).isEqualTo(disabledUser.getId());
        assertThat(enabledUser.getUserDetails().isEnabled()).isTrue();
    }

    @Test
    public void disablesUser() {

        UserDetails enabledUserDetails = UserDetails.builder()
                .username("enabled-now-disabled-later")
                .fullname("I was enabled, now I am not")
                .extension("enabled")
                .enabled(true)
                .build();

        User enabledUser = service.createUser(enabledUserDetails);
        assertThat(enabledUser).isNotNull();
        assertThat(enabledUser.getUserDetails().isEnabled()).isTrue();

        User disabledUser = service.disableUser(enabledUser.getId());

        assertThat(disabledUser).isNotNull();
        assertThat(disabledUser.getId()).isEqualTo(disabledUser.getId());
        assertThat(disabledUser.getUserDetails().isEnabled()).isFalse();
    }

    @Test
    public void getsExistingUser() {

        UserDetails existingUserDetails = UserDetails.builder()
                .username("existing")
                .fullname("Existing User")
                .extension("exist")
                .enabled(true)
                .build();

        User existingUser = service.createUser(existingUserDetails);

        User result = service.getExistingUser(existingUser.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(existingUser.getId());
    }

    @Test
    public void getsDisabledUserFullname() {

        UserDetails disabledUserDetails = UserDetails.builder()
                .username("disabled")
                .fullname("I am disabled")
                .extension("disabled")
                .enabled(false)
                .build();

        User disabledUser = service.createUser(disabledUserDetails);

        String fullname = service.getFullname(disabledUser.getId());

        assertThat(fullname).isNotNull().isEqualTo(disabledUserDetails.getFullname());
    }

    @Test
    public void getsEnabledUserFullname() {

        UserDetails enabledUserDetails = UserDetails.builder()
                .username("enabled")
                .fullname("I am enabled")
                .extension("enabled")
                .enabled(true)
                .build();

        User enabledUser = service.createUser(enabledUserDetails);

        String fullname = service.getFullname(enabledUser.getId());

        assertThat(fullname).isNotNull().isEqualTo(enabledUserDetails.getFullname());
    }
}
