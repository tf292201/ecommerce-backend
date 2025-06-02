package com.ecommerce.ecommerce_backend.controller;

import com.ecommerce.ecommerce_backend.dto.MessageResponseDTO;
import com.ecommerce.ecommerce_backend.dto.ShoppingCartDTO;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.service.ShoppingCartService;
import com.ecommerce.ecommerce_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    // USER - Get own cart
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ShoppingCartDTO> getCurrentUserCart(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ShoppingCartDTO cartDTO = shoppingCartService.getCartByUserId(user.getId());
            return ResponseEntity.ok(cartDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // USER - Add item to own cart
    @PostMapping("/me/add")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> addItemToCurrentUserCart(@RequestParam Long productId,
                                                    @RequestParam Integer quantity,
                                                    Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ShoppingCartDTO cartDTO = shoppingCartService.addItemToCart(user.getId(), productId, quantity);
            return ResponseEntity.ok(cartDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO("Error adding item to cart: " + e.getMessage()));
        }
    }

    // USER - Update item quantity in own cart
    @PutMapping("/me/update")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> updateItemInCurrentUserCart(@RequestParam Long productId,
                                                       @RequestParam Integer quantity,
                                                       Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ShoppingCartDTO cartDTO = shoppingCartService.updateItemQuantity(user.getId(), productId, quantity);
            return ResponseEntity.ok(cartDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO("Error updating item in cart: " + e.getMessage()));
        }
    }

    // USER - Remove item from own cart
    @DeleteMapping("/me/remove")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> removeItemFromCurrentUserCart(@RequestParam Long productId,
                                                         Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ShoppingCartDTO cartDTO = shoppingCartService.removeItemFromCart(user.getId(), productId);
            return ResponseEntity.ok(cartDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO("Error removing item from cart: " + e.getMessage()));
        }
    }

    // USER - Clear own cart
    @DeleteMapping("/me/clear")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> clearCurrentUserCart(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ShoppingCartDTO cartDTO = shoppingCartService.clearCart(user.getId());
            return ResponseEntity.ok(cartDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO("Error clearing cart: " + e.getMessage()));
        }
    }

    // ADMIN ONLY - Get any user's cart
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShoppingCartDTO> getUserCart(@PathVariable Long userId) {
        try {
            ShoppingCartDTO cartDTO = shoppingCartService.getCartByUserId(userId);
            return ResponseEntity.ok(cartDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}