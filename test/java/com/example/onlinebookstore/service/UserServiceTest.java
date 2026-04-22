package com.example.onlinebookstore.service;

import com.example.onlinebookstore.model.User;
import com.example.onlinebookstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserServiceImpl userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setName("Juan dela Cruz");
        sampleUser.setEmail("juan@example.com");
        sampleUser.setPassword("hashedPassword123");
        sampleUser.setAdmin(false);
    }

    // ── register ─────────────────────────────────────────

    @Test
    @DisplayName("register succeeds when email is not yet taken")
    void register_newEmail_savesAndReturnsUser() {
        when(userRepository.findByEmail("juan@example.com")).thenReturn(null);
        when(passwordEncoder.encode("rawPassword")).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        User input = new User();
        input.setName("Juan dela Cruz");
        input.setEmail("juan@example.com");
        input.setPassword("rawPassword");

        User result = userService.register(input);

        assertThat(result.getEmail()).isEqualTo("juan@example.com");
        assertThat(result.getName()).isEqualTo("Juan dela Cruz");
        verify(passwordEncoder).encode("rawPassword");
        verify(userRepository).save(input);
    }

    @Test
    @DisplayName("register encodes the password before saving")
    void register_passwordIsEncoded() {
        when(userRepository.findByEmail(anyString())).thenReturn(null);
        when(passwordEncoder.encode("mySecret")).thenReturn("$2a$encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User input = new User();
        input.setEmail("test@test.com");
        input.setPassword("mySecret");

        User result = userService.register(input);

        assertThat(result.getPassword()).isEqualTo("$2a$encoded");
        verify(passwordEncoder).encode("mySecret");
    }

    @Test
    @DisplayName("register throws when email is already in use")
    void register_duplicateEmail_throws() {
        when(userRepository.findByEmail("juan@example.com")).thenReturn(sampleUser);

        User input = new User();
        input.setEmail("juan@example.com");
        input.setPassword("pass");

        assertThatThrownBy(() -> userService.register(input))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already in use");

        verify(userRepository, never()).save(any());
    }

    // ── login ─────────────────────────────────────────────

    @Test
    @DisplayName("login succeeds with correct email and password")
    void login_correctCredentials_returnsUser() {
        when(userRepository.findByEmail("juan@example.com")).thenReturn(sampleUser);
        when(passwordEncoder.matches("rawPassword", "hashedPassword123")).thenReturn(true);

        User result = userService.login("juan@example.com", "rawPassword");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Juan dela Cruz");
    }

    @Test
    @DisplayName("login throws when email is not found")
    void login_emailNotFound_throws() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(null);

        assertThatThrownBy(() -> userService.login("unknown@example.com", "pass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No account found with that email");
    }

    @Test
    @DisplayName("login throws when password is incorrect")
    void login_wrongPassword_throws() {
        when(userRepository.findByEmail("juan@example.com")).thenReturn(sampleUser);
        when(passwordEncoder.matches("wrongPass", "hashedPassword123")).thenReturn(false);

        assertThatThrownBy(() -> userService.login("juan@example.com", "wrongPass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Incorrect password");
    }

    @Test
    @DisplayName("login does not expose which field failed for security")
    void login_returnsDistinctMessagesForEmailAndPassword() {
        // Email not found → different message than wrong password
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(null);
        when(userRepository.findByEmail("juan@example.com")).thenReturn(sampleUser);
        when(passwordEncoder.matches("bad", "hashedPassword123")).thenReturn(false);

        Throwable emailError = catchThrowable(() -> userService.login("nobody@example.com", "pass"));
        Throwable passError  = catchThrowable(() -> userService.login("juan@example.com", "bad"));

        assertThat(emailError).hasMessageContaining("No account found");
        assertThat(passError).hasMessageContaining("Incorrect password");
    }

    // ── getUserById ───────────────────────────────────────

    @Test
    @DisplayName("getUserById returns user when found")
    void getUserById_found_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        User result = userService.getUserById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("juan@example.com");
    }

    @Test
    @DisplayName("getUserById throws when user does not exist")
    void getUserById_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("getUserById respects the admin flag on the returned user")
    void getUserById_adminFlag_preserved() {
        sampleUser.setAdmin(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        User result = userService.getUserById(1L);

        assertThat(result.isAdmin()).isTrue();
    }
}
