package com.ecommerce.ecommerce_backend.controller;

import com.ecommerce.ecommerce_backend.dto.CartItemDTO;
import com.ecommerce.ecommerce_backend.dto.DTOMapper;
import com.ecommerce.ecommerce_backend.entity.CartItem;
import com.ecommerce.ecommerce_backend.service.CartItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cart-items")
@CrossOrigin(origins = "*")
public class CartItemController {

    @Autowired
    private CartItemService cartItemService;

    // Get all cart items for a specific cart
    @GetMapping("/cart/{cartId}")
    public ResponseEntity<List<CartItemDTO>> getCartItemsByCartId(@PathVariable Long cartId) {
        try {
            List<CartItem> cartItems = cartItemService.getCartItemsByCartId(cartId);
            List<CartItemDTO> cartItemDTOs = cartItems.stream()
                    .map(DTOMapper::toCartItemDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(cartItemDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get specific cart item by ID
    @GetMapping("/{id}")
    public ResponseEntity<CartItemDTO> getCartItemById(@PathVariable Long id) {
        try {
            CartItem cartItem = cartItemService.getCartItemById(id)
                    .orElseThrow(() -> new RuntimeException("Cart item not found"));
            
            CartItemDTO cartItemDTO = DTOMapper.toCartItemDTO(cartItem);
            return ResponseEntity.ok(cartItemDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Update cart item quantity
    @PutMapping("/{id}")
    public ResponseEntity<CartItemDTO> updateCartItem(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        try {
            CartItem cartItem = cartItemService.getCartItemById(id)
                    .orElseThrow(() -> new RuntimeException("Cart item not found"));

            cartItem.setQuantity(quantity);
            CartItem updatedCartItem = cartItemService.saveCartItem(cartItem);
            CartItemDTO cartItemDTO = DTOMapper.toCartItemDTO(updatedCartItem);
            return ResponseEntity.ok(cartItemDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // Delete cart item
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCartItem(@PathVariable Long id) {
        try {
            CartItem cartItem = cartItemService.getCartItemById(id)
                    .orElseThrow(() -> new RuntimeException("Cart item not found"));
            
            cartItemService.deleteCartItem(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}