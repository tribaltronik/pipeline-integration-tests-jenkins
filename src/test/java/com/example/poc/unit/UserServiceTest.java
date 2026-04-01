package com.example.poc.unit;

import com.example.poc.exception.UserNotFoundException;
import com.example.poc.model.User;
import com.example.poc.repository.UserRepository;
import com.example.poc.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("John Doe", "john@example.com");
        testUser.setId(1L);
    }

    @Nested
    @DisplayName("createUser")
    class CreateUserTests {

        @Test
        @DisplayName("should create user with valid data")
        void shouldCreateUserWithValidData() {
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.createUser("John Doe", "john@example.com");

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("John Doe");
            assertThat(result.getEmail()).isEqualTo("john@example.com");
            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            verify(userRepository, times(1)).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("should return user when found")
        void shouldReturnUserWhenFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            User result = userService.findById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findById(999L))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User not found with id: 999");
        }
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmailTests {

        @Test
        @DisplayName("should return user when email exists")
        void shouldReturnUserWhenEmailExists() {
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

            User result = userService.findByEmail("john@example.com");

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("should throw exception when email not found")
        void shouldThrowExceptionWhenEmailNotFound() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findByEmail("unknown@example.com"))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User not found with email");
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("should return all users")
        void shouldReturnAllUsers() {
            User user2 = new User("Jane Doe", "jane@example.com");
            user2.setId(2L);
            when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, user2));

            List<User> result = userService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(User::getName).containsExactly("John Doe", "Jane Doe");
        }

        @Test
        @DisplayName("should return empty list when no users")
        void shouldReturnEmptyListWhenNoUsers() {
            when(userRepository.findAll()).thenReturn(List.of());

            List<User> result = userService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUserTests {

        @Test
        @DisplayName("should update user successfully")
        void shouldUpdateUserSuccessfully() {
            User updatedUser = new User("John Updated", "john.updated@example.com");
            updatedUser.setId(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(updatedUser);

            User result = userService.updateUser(1L, "John Updated", "john.updated@example.com");

            assertThat(result.getName()).isEqualTo("John Updated");
            assertThat(result.getEmail()).isEqualTo("john.updated@example.com");
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUserTests {

        @Test
        @DisplayName("should delete user successfully")
        void shouldDeleteUserSuccessfully() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            doNothing().when(userRepository).delete(testUser);

            userService.deleteUser(1L);

            verify(userRepository, times(1)).delete(testUser);
        }
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmailTests {

        @Test
        @DisplayName("should return true when email exists")
        void shouldReturnTrueWhenEmailExists() {
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

            boolean result = userService.existsByEmail("john@example.com");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when email does not exist")
        void shouldReturnFalseWhenEmailDoesNotExist() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            boolean result = userService.existsByEmail("unknown@example.com");

            assertThat(result).isFalse();
        }
    }
}
