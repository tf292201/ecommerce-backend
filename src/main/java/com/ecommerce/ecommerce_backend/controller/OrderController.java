package com.ecommerce.ecommerce_backend.controller;

import com.ecommerce.ecommerce_backend.dto.*;
import com.ecommerce.ecommerce_backend.entity.Order;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.service.OrderService;
import com.ecommerce.ecommerce_backend.service.ShoppingCartService;
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
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ShoppingCartService shoppingCartService;

    // USER - Create order from cart (with Stripe payment processing)
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequestDTO request,
                                       Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get cart to check if it's empty
            ShoppingCartDTO cart = shoppingCartService.getCartByUserId(user.getId());
            
            if (cart.getCartItems().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponseDTO("Cart is empty"));
            }

            // Validate payment information is provided
            if (request.getCardToken() == null && request.getPaymentIntentId() == null) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponseDTO("Payment information is required"));
            }

            // Create order with Stripe payment processing (all handled in OrderService now)
            OrderDTO order = orderService.createOrderFromCart(user.getId(), request);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO("Error creating order: " + e.getMessage()));
        }
    }

    // USER - Get own orders
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<List<OrderDTO>> getCurrentUserOrders(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<OrderDTO> orders = orderService.getUserOrders(user.getId());
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // USER - Get specific order by ID (own orders only)
    @GetMapping("/me/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> getCurrentUserOrder(@PathVariable Long orderId, 
                                               Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            OrderDTO order = orderService.getOrderById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // Check if order belongs to current user
            if (!order.getUserId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new MessageResponseDTO("Access denied to this order"));
            }

            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ADMIN - Get all orders
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        try {
            List<OrderDTO> orders = orderService.getAllOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ADMIN - Get specific order by ID
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long orderId) {
        try {
            return orderService.getOrderById(orderId)
                    .map(order -> ResponseEntity.ok(order))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ADMIN - Update order status
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long orderId,
                                             @RequestParam String status) {
        try {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            OrderDTO updatedOrder = orderService.updateOrderStatus(orderId, orderStatus);
            return ResponseEntity.ok(updatedOrder);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO("Invalid order status: " + status));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO("Error updating order status: " + e.getMessage()));
        }
    }
}