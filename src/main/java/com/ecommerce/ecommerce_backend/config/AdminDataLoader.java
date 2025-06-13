package com.ecommerce.ecommerce_backend.config;

import com.ecommerce.ecommerce_backend.entity.Role;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminDataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // These values come from environment variables or application.properties
    @Value("${app.admin.username:}")
    private String adminUsername;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.first-name:System}")
    private String adminFirstName;

    @Value("${app.admin.last-name:Administrator}")
    private String adminLastName;

    @Override
    public void run(String... args) throws Exception {
        createDefaultAdminIfNeeded();
    }

    private void createDefaultAdminIfNeeded() {
        // Only create admin if:
        // 1. No admin users exist in the system
        // 2. Admin credentials are provided via environment variables
        
        boolean adminExists = userRepository.findAll().stream()
            .anyMatch(user -> user.getRole() == Role.ADMIN);
            
        if (adminExists) {
            System.out.println("ℹ️  Admin user already exists, skipping creation");
            return;
        }

        if (adminUsername == null || adminUsername.trim().isEmpty() ||
            adminPassword == null || adminPassword.trim().isEmpty() ||
            adminEmail == null || adminEmail.trim().isEmpty()) {
            
            System.out.println("⚠️  No admin credentials provided. To create an admin user, set:");
            System.out.println("   APP_ADMIN_USERNAME=your_admin_username");
            System.out.println("   APP_ADMIN_PASSWORD=your_admin_password");
            System.out.println("   APP_ADMIN_EMAIL=your_admin_email");
            System.out.println("   (or use application.properties)");
            return;
        }

        try {
            // Check if username or email already exists (as regular user)
            if (userRepository.existsByUsername(adminUsername)) {
                System.out.println("❌ Cannot create admin: Username '" + adminUsername + "' already exists");
                return;
            }

            if (userRepository.existsByEmail(adminEmail)) {
                System.out.println("❌ Cannot create admin: Email '" + adminEmail + "' already exists");
                return;
            }

            // Create admin user
            User admin = new User();
            admin.setUsername(adminUsername.trim());
            admin.setEmail(adminEmail.trim());
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setFirstName(adminFirstName);
            admin.setLastName(adminLastName);
            admin.setRole(Role.ADMIN);
            admin.setActive(true);

            userRepository.save(admin);
            
            System.out.println("✅ Default admin user created successfully!");
            System.out.println("   Username: " + adminUsername);
            System.out.println("   Email: " + adminEmail);
            System.out.println("   🚨 IMPORTANT: Change the default password immediately!");
            
        } catch (Exception e) {
            System.out.println("❌ Failed to create admin user: " + e.getMessage());
        }
    }
}