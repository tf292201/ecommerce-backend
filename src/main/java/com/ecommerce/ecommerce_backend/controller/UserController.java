package com.ecommerce.ecommerce_backend.controller;

import com.ecommerce.ecommerce_backend.dto.DTOMapper;
import com.ecommerce.ecommerce_backend.dto.MessageResponseDTO;
import com.ecommerce.ecommerce_backend.dto.UserRequestDTO;
import com.ecommerce.ecommerce_backend.dto.UserResponseDTO;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    // ADMIN ONLY - Get all users
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserResponseDTO> userDTOs = users.stream()
                .map(DTOMapper::toUserResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDTOs);
    }

    // ADMIN ONLY - Get user by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(DTOMapper.toUserResponseDTO(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    // USER - Get own profile
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<UserResponseDTO> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userService.getUserByUsername(username)
                .map(user -> ResponseEntity.ok(DTOMapper.toUserResponseDTO(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    // USER - Update own profile
    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> updateCurrentUser(@Valid @RequestBody UserRequestDTO userRequestDTO, 
                                             Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Create updated user object
            User userDetails = new User();
            userDetails.setFirstName(userRequestDTO.getFirstName());
            userDetails.setLastName(userRequestDTO.getLastName());
            userDetails.setEmail(userRequestDTO.getEmail());
            userDetails.setPhoneNumber(userRequestDTO.getPhoneNumber());
            userDetails.setPassword(userRequestDTO.getPassword()); // Only if provided

            User updatedUser = userService.updateUserProfile(currentUser, userDetails);
            UserResponseDTO userResponseDTO = DTOMapper.toUserResponseDTO(updatedUser);
            
            return ResponseEntity.ok(userResponseDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO("Error updating profile: " + e.getMessage()));
        }
    }

    // ADMIN ONLY - Update any user
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id, 
                                                    @Valid @RequestBody UserRequestDTO userRequestDTO) {
        User userDetails = new User();
        userDetails.setUsername(userRequestDTO.getUsername());
        userDetails.setEmail(userRequestDTO.getEmail());
        userDetails.setFirstName(userRequestDTO.getFirstName());
        userDetails.setLastName(userRequestDTO.getLastName());
        userDetails.setPhoneNumber(userRequestDTO.getPhoneNumber());
        userDetails.setPassword(userRequestDTO.getPassword());

        User updatedUser = userService.updateUser(id, userDetails);
        UserResponseDTO userResponseDTO = DTOMapper.toUserResponseDTO(updatedUser);
        
        return ResponseEntity.ok(userResponseDTO);
    }

    // ADMIN ONLY - Delete user
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(new MessageResponseDTO("User deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO("Error deleting user: " + e.getMessage()));
        }
    }

    // ADMIN ONLY - Get user by username
    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(user -> ResponseEntity.ok(DTOMapper.toUserResponseDTO(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ADMIN ONLY - Get user by email
    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(user -> ResponseEntity.ok(DTOMapper.toUserResponseDTO(user)))
                .orElse(ResponseEntity.notFound().build());
    }
}