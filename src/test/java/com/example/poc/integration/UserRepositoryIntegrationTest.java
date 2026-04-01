package com.example.poc.integration;

import com.example.poc.model.User;
import com.example.poc.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@DisplayName("UserRepository Integration Tests (with Testcontainers)")
class UserRepositoryIntegrationTest {

    @SuppressWarnings("rawtypes")
    @Container
    static OracleContainer oracleContainer = new OracleContainer(
            DockerImageName.parse("gvenzl/oracle-xe:21-slim")
                    .asCompatibleSubstituteFor("gvenzl/oracle-xe"))
            .withOracleSid("ORCL")
            .withReuse(false)
            .waitingFor(new LogMessageWaitStrategy()
                    .withRegEx(".*DATABASE IS READY TO USE.*")
                    .withStartupTimeout(Duration.ofMinutes(5)));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> 
            "jdbc:oracle:thin:@//localhost:" + oracleContainer.getMappedPort(1521) + "/ORCL");
        registry.add("spring.datasource.username", () -> "system");
        registry.add("spring.datasource.password", () -> "oracle");
        registry.add("spring.datasource.driver-class-name", () -> "oracle.jdbc.OracleDriver");
    }


    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        testUser = new User("Test User", "test@example.com");
    }

    @Nested
    @DisplayName("save")
    class SaveTests {

        @Test
        @DisplayName("should persist user with generated ID")
        void shouldPersistUserWithGeneratedId() {
            User saved = userRepository.save(testUser);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getId()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should set default status to ACTIVE")
        void shouldSetDefaultStatusToActive() {
            User saved = userRepository.save(testUser);

            assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("should find user by ID")
        void shouldFindUserById() {
            User saved = userRepository.save(testUser);

            Optional<User> found = userRepository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("should return empty for non-existent ID")
        void shouldReturnEmptyForNonExistentId() {
            Optional<User> found = userRepository.findById(999999L);

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmailTests {

        @Test
        @DisplayName("should find user by exact email")
        void shouldFindUserByExactEmail() {
            userRepository.save(testUser);

            Optional<User> found = userRepository.findByEmail("test@example.com");

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Test User");
        }

        @Test
        @DisplayName("should return empty for non-existent email")
        void shouldReturnEmptyForNonExistentEmail() {
            Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("should return all saved users")
        void shouldReturnAllSavedUsers() {
            User user1 = new User("User One", "user1@example.com");
            User user2 = new User("User Two", "user2@example.com");
            userRepository.save(user1);
            userRepository.save(user2);

            List<User> all = userRepository.findAll();

            assertThat(all).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no users")
        void shouldReturnEmptyListWhenNoUsers() {
            List<User> all = userRepository.findAll();

            assertThat(all).isEmpty();
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("should delete existing user")
        void shouldDeleteExistingUser() {
            User saved = userRepository.save(testUser);

            userRepository.delete(saved);

            assertThat(userRepository.findById(saved.getId())).isEmpty();
        }

        @Test
        @DisplayName("should delete all users")
        void shouldDeleteAllUsers() {
            userRepository.save(testUser);
            userRepository.save(new User("Another User", "another@example.com"));

            userRepository.deleteAll();

            assertThat(userRepository.findAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTests {

        @Test
        @DisplayName("should perform full CRUD cycle")
        void shouldPerformFullCrudCycle() {
            User user = new User("CRUD Test", "crud@example.com");
            
            User created = userRepository.save(user);
            assertThat(created.getId()).isNotNull();

            Optional<User> read = userRepository.findById(created.getId());
            assertThat(read).isPresent();
            assertThat(read.get().getName()).isEqualTo("CRUD Test");

            read.get().setName("Updated Name");
            User updated = userRepository.save(read.get());
            assertThat(updated.getName()).isEqualTo("Updated Name");

            userRepository.delete(updated);
            assertThat(userRepository.findById(updated.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Multiple Saves")
    class MultipleSavesTests {

        @Test
        @DisplayName("should handle multiple concurrent saves")
        void shouldHandleMultipleConcurrentSaves() {
            for (int i = 0; i < 10; i++) {
                User user = new User("User " + i, "user" + i + "@example.com");
                userRepository.save(user);
            }

            List<User> all = userRepository.findAll();
            assertThat(all).hasSize(10);
        }
    }
}
