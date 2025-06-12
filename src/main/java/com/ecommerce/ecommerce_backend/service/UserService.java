package com.ecommerce.ecommerce_backend.service;

import com.ecommerce.ecommerce_backend.entity.Role;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("🔍 UserService.loadUserByUsername called with: " + username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        System.out.println("✅ User found: " + user.getUsername());
        System.out.println("🔐 Stored password hash: " + user.getPassword());
        System.out.println("👤 User authorities: " + user.getAuthorities());
        System.out.println("🎯 User enabled: " + user.isEnabled());
        
        return user;
    }

    // Add a method to test password matching
    public boolean testPasswordMatch(String username, String rawPassword) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String storedHash = user.getPassword();
            boolean matches = passwordEncoder.matches(rawPassword, storedHash);
            
            System.out.println("🧪 PASSWORD MATCH TEST 🧪");
            System.out.println("Username: " + username);
            System.out.println("Raw password: '" + rawPassword + "'");
            System.out.println("Stored hash: " + storedHash);
            System.out.println("Password matches: " + matches);
            System.out.println("Password encoder: " + passwordEncoder.getClass().getSimpleName());
            
            return matches;
        }
        return false;
    }

    // Rest of your existing methods...
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User createUser(User user) {
        String rawPassword = user.getPassword();
        String encodedPassword = passwordEncoder.encode(rawPassword);
        
        System.out.println("🆕 CREATING NEW USER 🆕");
        System.out.println("Username: " + user.getUsername());
        System.out.println("Raw password: '" + rawPassword + "'");
        System.out.println("Encoded password: " + encodedPassword);
        
        user.setPassword(encodedPassword);
        if (user.getRole() == null) {
            user.setRole(Role.CUSTOMER);
        }
        return userRepository.save(user);
    }

    public User createAdmin(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.ADMIN);
        return userRepository.save(user);
    }

    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setPhoneNumber(userDetails.getPhoneNumber());

        // Only update password if it's provided and not empty
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        return userRepository.save(user);
    }

    public User updateUserProfile(User currentUser, User userDetails) {
        currentUser.setFirstName(userDetails.getFirstName());
        currentUser.setLastName(userDetails.getLastName());
        currentUser.setPhoneNumber(userDetails.getPhoneNumber());
        currentUser.setEmail(userDetails.getEmail());

        // Only update password if it's provided and not empty
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            currentUser.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        return userRepository.save(currentUser);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
    }

    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}