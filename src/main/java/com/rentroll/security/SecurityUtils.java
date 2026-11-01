package com.rentroll.security;

import com.rentroll.entity.Role;
import com.rentroll.entity.User;
import com.rentroll.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Utility helper to fetch currently logged-in user details in controllers and services.
 */
@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get the currently authenticated User entity from database.
     */
    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }

        if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userRepository.findById(userDetails.getId());
        }

        return userRepository.findByEmail(auth.getName());
    }

    /**
     * Get current authenticated user or throw IllegalStateException.
     */
    public User getRequiredCurrentUser() {
        return getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Operation requires an authenticated user session"));
    }

    /**
     * Checks if current user is an Admin.
     */
    public boolean isAdmin() {
        return getCurrentUser().map(u -> u.getRole() == Role.ADMIN).orElse(false);
    }

    /**
     * Checks if current user is a Landlord.
     */
    public boolean isLandlord() {
        return getCurrentUser().map(u -> u.getRole() == Role.LANDLORD).orElse(false);
    }

    /**
     * Checks if current user is a Tenant.
     */
    public boolean isTenant() {
        return getCurrentUser().map(u -> u.getRole() == Role.TENANT).orElse(false);
    }
}

