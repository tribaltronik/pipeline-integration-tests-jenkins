package com.example.poc.service;

import com.example.poc.exception.UserNotFoundException;
import com.example.poc.model.User;
import com.example.poc.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String name, String email) {
        User user = new User(name, email);
        return userRepository.save(user);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> findByStatus(String status) {
        return userRepository.findByStatus(status);
    }

    public List<User> searchByName(String name) {
        return userRepository.findByNameContainingIgnoreCase(name);
    }

    public User updateUser(Long id, String name, String email) {
        User user = findById(id);
        user.setName(name);
        user.setEmail(email);
        return userRepository.save(user);
    }

    public User deactivateUser(Long id) {
        User user = findById(id);
        user.setStatus("INACTIVE");
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        User user = findById(id);
        userRepository.delete(user);
    }

    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    public long countActiveUsers() {
        return userRepository.findByStatus("ACTIVE").size();
    }
}
