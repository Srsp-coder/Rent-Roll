package com.rentroll.service;

import com.rentroll.entity.Role;
import com.rentroll.entity.User;
import com.rentroll.exception.BusinessRuleException;
import com.rentroll.exception.ResourceNotFoundException;
import com.rentroll.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service handling user account management, password hashing, and role retrieval.
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    public User createUser(User user, String rawPassword) {
        // Enforce email uniqueness
        String cleanEmail = user.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new BusinessRuleException("An account with email " + cleanEmail + " already exists");
        }

        user.setEmail(cleanEmail);
        user.setPassword(passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    public User updateUser(Long id, String fullName, String phoneNumber, Role role) {
        User existing = getUserById(id);
        existing.setFullName(fullName);
        existing.setPhoneNumber(phoneNumber);
        if (role != null) {
            existing.setRole(role);
        }
        return userRepository.save(existing);
    }

    public void toggleUserActive(Long id) {
        User user = getUserById(id);
        // Toggle the boolean active flag
        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    public List<User> getLandlords() {
        return userRepository.findByRoleAndActiveTrue(Role.LANDLORD);
    }

    public List<User> getTenants() {
        return userRepository.findByRoleAndActiveTrue(Role.TENANT);
    }

    public long countUsers() {
        return userRepository.count();
    }
}

