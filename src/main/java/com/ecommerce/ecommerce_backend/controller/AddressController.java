package com.ecommerce.ecommerce_backend.controller;

import com.ecommerce.ecommerce_backend.dto.AddressRequestDTO;
import com.ecommerce.ecommerce_backend.dto.AddressResponseDTO;
import com.ecommerce.ecommerce_backend.dto.MessageResponseDTO;
import com.ecommerce.ecommerce_backend.entity.Address;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.service.AddressService;
import com.ecommerce.ecommerce_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@CrossOrigin(origins = "*")
public class AddressController {
    
    @Autowired
    private AddressService addressService;
    
    @Autowired
    private UserService userService;
    
    // Get all addresses for current user
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<List<AddressResponseDTO>> getCurrentUserAddresses(Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            List<AddressResponseDTO> addresses = addressService.getUserAddresses(user);
            return ResponseEntity.ok(addresses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Get addresses by type for current user
    @GetMapping("/me/type/{type}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<List<AddressResponseDTO>> getCurrentUserAddressesByType(
            @PathVariable String type, Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Address.AddressType addressType = Address.AddressType.valueOf(type.toUpperCase());
            List<AddressResponseDTO> addresses = addressService.getUserAddressesByType(user, addressType);
            return ResponseEntity.ok(addresses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Get default address for current user
    @GetMapping("/me/default")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<AddressResponseDTO> getCurrentUserDefaultAddress(Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            return addressService.getDefaultAddress(user)
                    .map(address -> ResponseEntity.ok(address))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Get specific address by ID
    @GetMapping("/me/{addressId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<AddressResponseDTO> getCurrentUserAddress(
            @PathVariable Long addressId, Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            return addressService.getAddressById(user, addressId)
                    .map(address -> ResponseEntity.ok(address))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Create new address
    @PostMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> createAddress(@Valid @RequestBody AddressRequestDTO requestDTO,
                                         Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            AddressResponseDTO address = addressService.createAddress(user, requestDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(address);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO("Error creating address: " + e.getMessage()));
        }
    }
    
    // Update existing address
    @PutMapping("/me/{addressId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> updateAddress(@PathVariable Long addressId,
                                         @Valid @RequestBody AddressRequestDTO requestDTO,
                                         Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            AddressResponseDTO address = addressService.updateAddress(user, addressId, requestDTO);
            return ResponseEntity.ok(address);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO("Error updating address: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponseDTO("Internal server error"));
        }
    }
    
    // Set address as default
    @PutMapping("/me/{addressId}/default")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> setAddressAsDefault(@PathVariable Long addressId,
                                               Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            AddressResponseDTO address = addressService.setAsDefault(user, addressId);
            return ResponseEntity.ok(address);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO("Error setting default address: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponseDTO("Internal server error"));
        }
    }
    
    // Delete address
    @DeleteMapping("/me/{addressId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> deleteAddress(@PathVariable Long addressId,
                                         Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            addressService.deleteAddress(user, addressId);
            return ResponseEntity.ok(new MessageResponseDTO("Address deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO("Error deleting address: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponseDTO("Internal server error"));
        }
    }
    
    // Helper method to get current user
    private User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}