package ru.practicum.shareit.RepositoryTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Integration Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setName("John Doe");
        user.setEmail("john@example.com");
    }

    @Test
    @DisplayName("Should save user")
    void shouldSaveUser() {
        // When
        User savedUser = userRepository.save(user);

        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getName()).isEqualTo(user.getName());
        assertThat(savedUser.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        // Given
        User savedUser = em.persistAndFlush(user);

        // When
        Optional<User> foundUser = userRepository.findByEmail(savedUser.getEmail());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getId()).isEqualTo(savedUser.getId());
        assertThat(foundUser.get().getEmail()).isEqualTo(savedUser.getEmail());
    }

    @Test
    @DisplayName("Should return empty when email not found")
    void shouldReturnEmptyWhenEmailNotFound() {
        // When
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("Should update user")
    void shouldUpdateUser() {
        // Given
        User savedUser = em.persistAndFlush(user);
        em.clear();

        // When
        savedUser.setName("Updated Name");
        savedUser.setEmail("updated@example.com");
        User updatedUser = userRepository.save(savedUser);

        // Then
        User foundUser = em.find(User.class, savedUser.getId());
        assertThat(foundUser.getName()).isEqualTo("Updated Name");
        assertThat(foundUser.getEmail()).isEqualTo("updated@example.com");
    }

    @Test
    @DisplayName("Should delete user")
    void shouldDeleteUser() {
        // Given
        User savedUser = em.persistAndFlush(user);

        // When
        userRepository.deleteById(savedUser.getId());
        em.flush();

        // Then
        User foundUser = em.find(User.class, savedUser.getId());
        assertThat(foundUser).isNull();
    }
}