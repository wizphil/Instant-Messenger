package com.wizphil.instantmessenger.cache;

import com.wizphil.instantmessenger.persistence.user.User;
import com.wizphil.instantmessenger.persistence.user.UserDetails;
import com.wizphil.instantmessenger.persistence.user.UserSettings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
public class UserCacheTests {
    @Autowired
    UserCache cache;

    @Test
    public void createsUser() {

        User carol = cache.createUser(User.builder()
                .userSettings(UserSettings.defaultSettings())
                .userDetails(UserDetails.builder().username("carol").fullname("Carol Baskins").enabled(true).extension("TIGER").build())
                .build());

        User result = cache.get(carol.getId());

        assertThat(result).isNotNull();
        assertThat(result.getUserDetails().getUsername()).isEqualTo(carol.getUserDetails().getUsername());
    }

    @Test
    public void findsByUsername() {

        User alice = cache.createUser(User.builder()
                .userSettings(UserSettings.defaultSettings())
                .userDetails(UserDetails.builder().username("alice").fullname("Alice 1").enabled(true).extension("42").build())
                .build());

        User result = cache.getByUsername(alice.getUserDetails().getUsername());

        assertThat(result).isNotNull();
        assertThat(result.getUserDetails().getUsername()).isEqualTo(alice.getUserDetails().getUsername());
    }

    @Test
    public void updatesFullnameOnSave() {

        User bob = cache.createUser(User.builder()
                .userSettings(UserSettings.defaultSettings())
                .userDetails(UserDetails.builder().username("bob").fullname("Bob Porter").enabled(true).extension("ospace").build())
                .build());

        String newName = "the Bobs";
        bob.getUserDetails().setFullname(newName);
        bob = cache.updateUser(bob);

        assertThat(bob).isNotNull();
        assertThat(bob.getUserDetails().getFullname()).isEqualTo(newName);
    }
}
