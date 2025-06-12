package com.ecommerce.ecommerce_backend.service;

import com.ecommerce.ecommerce_backend.dto.DTOMapper;
import com.ecommerce.ecommerce_backend.dto.ShoppingCartDTO;
import com.ecommerce.ecommerce_backend.entity.CartItem;
import com.ecommerce.ecommerce_backend.entity.Product;
import com.ecommerce.ecommerce_backend.entity.ShoppingCart;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.repository.CartItemRepository;
import com.ecommerce.ecommerce_backend.repository.ShoppingCartRepository;
import com.ecommerce.ecommerce_backend.repository.UserRepository;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class ShoppingCartService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShoppingCartRepository shoppingCartRepository;

    @Autowired
    private CartItemService cartItemService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private CartItemRepository cartItemRepository;

    public ShoppingCartDTO getCartByUserId(Long userId) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ShoppingCart cart = user.getShoppingCart();
        if (cart == null) {
            // Create cart if it doesn't exist
            cart = createCartForUser(user);
        }

        return DTOMapper.toShoppingCartDTO(cart);
    }

    public ShoppingCartDTO addItemToCart(Long userId, Long productId, Integer quantity) {
        // 🔧 VALIDATION: Check for positive quantity
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive number");
        }
    
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    
        Product product = productService.getProductById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    
        // 🔧 VALIDATION: Check stock availability
        if (quantity > product.getStockQuantity()) {
            throw new IllegalArgumentException("Requested quantity (" + quantity + 
                ") exceeds available stock (" + product.getStockQuantity() + ")");
        }
    
        ShoppingCart cart = user.getShoppingCart();
        if (cart == null) {
            cart = createCartForUser(user);
        }
    
        // Check if item already exists in cart
        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();
    
        if (existingItem.isPresent()) {
            // Update quantity and recalculate total price
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;
            
            // 🔧 VALIDATION: Check total quantity doesn't exceed stock
            if (newQuantity > product.getStockQuantity()) {
                throw new IllegalArgumentException("Total quantity (" + newQuantity + 
                    ") would exceed available stock (" + product.getStockQuantity() + ")");
            }
            
            item.setQuantity(newQuantity);
            // Recalculate total price
            item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            cartItemService.saveCartItem(item);
        } else {
            // Add new item with price calculations
            CartItem cartItem = new CartItem();
            cartItem.setShoppingCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
            
            // SET THE PRICE FIELDS
            cartItem.setUnitPrice(product.getPrice());
            cartItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
            
            cartItemService.saveCartItem(cartItem);
            cart.getCartItems().add(cartItem);
        }
    
        // 🔧 RECALCULATE CART TOTAL
        recalculateCartTotal(cart);
        cart = shoppingCartRepository.save(cart);
        return DTOMapper.toShoppingCartDTO(cart);
    }
    
    // 🔧 ADD THIS HELPER METHOD
    private void recalculateCartTotal(ShoppingCart cart) {
        BigDecimal total = cart.getCartItems().stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalAmount(total);
    }
    public ShoppingCartDTO updateItemQuantity(Long userId, Long productId, Integer quantity) {
        // 🔧 VALIDATION: Handle zero/negative quantities
        if (quantity != null && quantity <= 0) {
            // Remove item instead of setting negative quantity
            return removeItemFromCart(userId, productId);
        }
        
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }
    
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    
        ShoppingCart cart = user.getShoppingCart();
        if (cart == null) {
            throw new RuntimeException("Cart not found");
        }
    
        CartItem cartItem = cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));
    
        // 🔧 VALIDATION: Check stock availability
        Product product = cartItem.getProduct();
        if (quantity > product.getStockQuantity()) {
            throw new IllegalArgumentException("Requested quantity (" + quantity + 
                ") exceeds available stock (" + product.getStockQuantity() + ")");
        }
    
        cartItem.setQuantity(quantity);
        // RECALCULATE TOTAL PRICE
        cartItem.setTotalPrice(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
        cartItemService.saveCartItem(cartItem);
    
        // 🔧 RECALCULATE CART TOTAL
        recalculateCartTotal(cart);
        cart = shoppingCartRepository.save(cart);
        return DTOMapper.toShoppingCartDTO(cart);
    }
    public ShoppingCartDTO removeItemFromCart(Long userId, Long productId) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ShoppingCart cart = user.getShoppingCart();
        if (cart == null) {
            throw new RuntimeException("Cart not found");
        }

        CartItem cartItem = cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        cart.getCartItems().remove(cartItem);
        cartItemService.deleteCartItem(cartItem.getId());

        cart = shoppingCartRepository.save(cart);
        return DTOMapper.toShoppingCartDTO(cart);
    }

    @Transactional
    public ShoppingCartDTO clearCart(Long userId) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    
        ShoppingCart cart = user.getShoppingCart();
        if (cart == null) {
            throw new RuntimeException("Cart not found");
        }
    
        // This will now work because of @Modifying annotation
        cartItemRepository.deleteByShoppingCartId(cart.getId());
        
        // Reset total amount
        cart.setTotalAmount(BigDecimal.ZERO);
        cart = shoppingCartRepository.save(cart);
        
        return DTOMapper.toShoppingCartDTO(cart);
    }

    private ShoppingCart createCartForUser(User user) {
        ShoppingCart cart = new ShoppingCart();
        cart.setUser(user);
        cart = shoppingCartRepository.save(cart);
        user.setShoppingCart(cart);
        userRepository.save(user);
        return cart;
    }

    public ShoppingCart saveShoppingCart(ShoppingCart shoppingCart) {
        return shoppingCartRepository.save(shoppingCart);
    }
}