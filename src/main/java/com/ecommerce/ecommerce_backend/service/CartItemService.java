package com.ecommerce.ecommerce_backend.service;

import com.ecommerce.ecommerce_backend.entity.CartItem;
import com.ecommerce.ecommerce_backend.repository.CartItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CartItemService {

    @Autowired
    private CartItemRepository cartItemRepository;

    public List<CartItem> getAllCartItems() {
        return cartItemRepository.findAll();
    }

    public Optional<CartItem> getCartItemById(Long id) {
        return cartItemRepository.findById(id);
    }
    

    public List<CartItem> getCartItemsByCartId(Long cartId) {
        return cartItemRepository.findByShoppingCartId(cartId);
    }

    // Save or update cart item
    public CartItem saveCartItem(CartItem cartItem) {
        return cartItemRepository.save(cartItem);
    }

    // Delete cart item by ID
    public void deleteCartItem(Long id) {
        cartItemRepository.deleteById(id);
    }

    // Delete cart item entity
    public void deleteCartItem(CartItem cartItem) {
        cartItemRepository.delete(cartItem);
    }

    // Delete all cart items by shopping cart ID
    public void deleteAllByShoppingCartId(Long cartId) {
         cartItemRepository.deleteByShoppingCartId(cartId);
    }

    // Update cart item quantity
    public CartItem updateCartItemQuantity(Long id, Integer quantity) {
        CartItem cartItem = cartItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        
        cartItem.setQuantity(quantity);
        return cartItemRepository.save(cartItem);
    }

    // Check if cart item exists
    public boolean existsById(Long id) {
        return cartItemRepository.existsById(id);
    }
}