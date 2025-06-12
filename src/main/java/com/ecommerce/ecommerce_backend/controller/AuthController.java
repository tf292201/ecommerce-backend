package com.ecommerce.ecommerce_backend.controller;

import com.ecommerce.ecommerce_backend.dto.*;
import com.ecommerce.ecommerce_backend.entity.Role;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.security.JwtUtil;
import com.ecommerce.ecommerce_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequestDTO registerRequest) {
        try {
            // Check if username already exists
            if (userService.existsByUsername(registerRequest.getUsername())) {
                return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO("Error: Username is already taken!"));
            }

            // Check if email already exists
            if (userService.existsByEmail(registerRequest.getEmail())) {
                return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO("Error: Email is already in use!"));
            }

            // Create new user
            User user = new User(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                registerRequest.getFirstName(),
                registerRequest.getLastName()
            );
            user.setPhoneNumber(registerRequest.getPhoneNumber());

            User savedUser = userService.createUser(user);

            // Generate JWT token
            UserDetails userDetails = userService.loadUserByUsername(savedUser.getUsername());
            String jwt = jwtUtil.generateToken(userDetails);

            // Return success response with token
            UserResponseDTO userResponseDTO = DTOMapper.toUserResponseDTO(savedUser);
            AuthResponseDTO authResponse = new AuthResponseDTO(jwt, userResponseDTO);

            return ResponseEntity.ok(authResponse);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponseDTO("Error: Could not create user. " + e.getMessage()));
        }
    }

    @PostMapping("/login")
public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequestDTO loginRequest) {
    System.out.println("🚀 LOGIN ATTEMPT 🚀");
    System.out.println("Username: " + loginRequest.getUsername());
    System.out.println("Password: '" + loginRequest.getPassword() + "'");
    
    try {
        // Test password matching before authentication
        boolean passwordMatches = userService.testPasswordMatch(loginRequest.getUsername(), loginRequest.getPassword());
        System.out.println("🔐 Manual password test result: " + passwordMatches);
        
        // Authenticate user
        System.out.println("🔄 Attempting Spring Security authentication...");
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            )
        );
        System.out.println("✅ Spring Security authentication successful!");

        // Load user details
        System.out.println("🔍 Loading user details...");
        UserDetails userDetails = userService.loadUserByUsername(loginRequest.getUsername());
        System.out.println("✅ UserDetails loaded: " + userDetails.getUsername());
        
        System.out.println("🔍 Getting user entity...");
        User user = userService.getUserByUsername(loginRequest.getUsername()).get();
        System.out.println("✅ User entity loaded: " + user.getUsername());

        // Generate JWT token
        System.out.println("🔑 Generating JWT token...");
        String jwt = jwtUtil.generateToken(userDetails);
        System.out.println("✅ JWT token generated: " + jwt.substring(0, 20) + "...");

        // Return success response with token
        System.out.println("📦 Creating response DTO...");
        UserResponseDTO userResponseDTO = DTOMapper.toUserResponseDTO(user);
        System.out.println("✅ UserResponseDTO created");
        
        AuthResponseDTO authResponse = new AuthResponseDTO(jwt, userResponseDTO);
        System.out.println("✅ AuthResponseDTO created");
        
        System.out.println("🎉 Login successful! Returning response...");
        return ResponseEntity.ok(authResponse);

    } catch (BadCredentialsException e) {
        System.out.println("❌ BadCredentialsException: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new MessageResponseDTO("Error: Invalid username or password!"));
    } catch (Exception e) {
        System.out.println("❌ Authentication Exception: " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new MessageResponseDTO("Error: Authentication failed. " + e.getMessage()));
    }
}

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO("Error: Invalid token format!"));
            }

            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);

            if (username != null && jwtUtil.validateToken(token)) {
                UserDetails userDetails = userService.loadUserByUsername(username);
                String newToken = jwtUtil.generateToken(userDetails);
                
                User user = userService.getUserByUsername(username).get();
                UserResponseDTO userResponseDTO = DTOMapper.toUserResponseDTO(user);
                AuthResponseDTO authResponse = new AuthResponseDTO(newToken, userResponseDTO);

                return ResponseEntity.ok(authResponse);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponseDTO("Error: Invalid or expired token!"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponseDTO("Error: Token refresh failed. " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        // Since we're using stateless JWT, logout is handled on the client side
        // by simply removing the token from storage
        return ResponseEntity.ok(new MessageResponseDTO("User logged out successfully!"));
    }

    // Add this method to your AuthController.java (TEMPORARY - remove after creating admin)

@PostMapping("/create-admin")
public ResponseEntity<?> createAdmin(@Valid @RequestBody RegisterRequestDTO registerRequest) {
    try {
        // Check if username already exists
        if (userService.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.badRequest()
                .body(new MessageResponseDTO("Error: Username is already taken!"));
        }

        // Check if email already exists
        if (userService.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest()
                .body(new MessageResponseDTO("Error: Email is already in use!"));
        }

        // Create new admin user
        /// DELETE LATER !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        User user = new User(
            registerRequest.getUsername(),
            registerRequest.getEmail(),
            registerRequest.getPassword(),
            registerRequest.getFirstName(),
            registerRequest.getLastName()
        );
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setRole(Role.ADMIN); // Set as ADMIN instead of CUSTOMER

        User savedUser = userService.createUser(user);

        // Generate JWT token
        UserDetails userDetails = userService.loadUserByUsername(savedUser.getUsername());
        String jwt = jwtUtil.generateToken(userDetails);

        // Return success response with token
        UserResponseDTO userResponseDTO = DTOMapper.toUserResponseDTO(savedUser);
        AuthResponseDTO authResponse = new AuthResponseDTO(jwt, userResponseDTO);

        return ResponseEntity.ok(authResponse);

    } catch (Exception e) {
        return ResponseEntity.badRequest()
            .body(new MessageResponseDTO("Error: Could not create admin user. " + e.getMessage()));
    }
}
}