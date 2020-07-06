package com.wizphil.instantmessenger.repository;

import com.wizphil.instantmessenger.persistence.user.User;
import com.wizphil.instantmessenger.persistence.user.UserDetails;
import com.wizphil.instantmessenger.persistence.user.UserSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
public class UserRepositoryTests {
    @Autowired
    UserRepository repository;

    User alice, bob, eve;

    @BeforeEach
    public void setup() {
        repository.deleteAll();
        alice = repository.save(User.builder()
                .userSettings(UserSettings.defaultSettings())
                .userDetails(UserDetails.builder().username("alice").fullname("Alice 1").enabled(true).extension("42").build())
                .build());

        bob = repository.save(User.builder()
                .userSettings(UserSettings.defaultSettings())
                .userDetails(UserDetails.builder().username("bob").fullname("Bob Slydell").enabled(true).extension("TPS").build())
                .build());

        eve = repository.save(User.builder()
                .userSettings(UserSettings.defaultSettings())
                .userDetails(UserDetails.builder().username("eve").fullname("Killing Eve").enabled(true).extension("2018").build())
                .build());
    }

    @Test
    public void setsIdOnSave() {

        User carol = repository.save(User.builder()
                .userSettings(UserSettings.defaultSettings())
                .userDetails(UserDetails.builder().username("carol").fullname("Carol Baskins").enabled(true).extension("TIGER").build())
                .build());

        assertThat(carol).isNotNull();
        assertThat(carol.getId()).isNotNull();
    }

    @Test
    public void findsByUsername() {

        User result = repository.findFirstByUserDetails_Username("alice");

        assertThat(result).isNotNull();
        assertThat(result.getUserDetails().getUsername()).isEqualTo(alice.getUserDetails().getUsername());
    }

    @Test
    public void updatesFullnameOnSave() {

        String newName = "Villanelle";
        alice.getUserDetails().setFullname(newName);
        alice = repository.save(alice);

        assertThat(alice).isNotNull();
        assertThat(alice.getUserDetails().getFullname()).isEqualTo(newName);
    }
}
