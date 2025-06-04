package com.ecommerce.ecommerce_backend.service;

import com.ecommerce.ecommerce_backend.dto.CreateOrderRequestDTO;
import com.ecommerce.ecommerce_backend.dto.DTOMapper;
import com.ecommerce.ecommerce_backend.dto.OrderDTO;
import com.ecommerce.ecommerce_backend.entity.*;
import com.ecommerce.ecommerce_backend.repository.OrderRepository;
import com.stripe.model.PaymentIntent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ShoppingCartService shoppingCartService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ProductService productService;

    @Autowired
    private StripePaymentService stripePaymentService;

    /**
     * Creates an order from the user's shopping cart with Stripe payment processing
     */
    @Transactional
    public OrderDTO createOrderFromCart(Long userId, CreateOrderRequestDTO request) {
        // Get user and validate cart
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        ShoppingCart cart = user.getShoppingCart();
        if (cart == null || cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Cannot create order from empty cart");
        }

        // Validate stock for all items
        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();
            if (cartItem.getQuantity() > product.getStockQuantity()) {
                throw new RuntimeException(
                    "Insufficient stock for product: " + product.getName() + 
                    ". Available: " + product.getStockQuantity() + 
                    ", Requested: " + cartItem.getQuantity()
                );
            }
        }

        // Calculate total amount
        BigDecimal totalAmount = cart.getCartItems().stream()
            .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        try {
            // 🔥 PROCESS STRIPE PAYMENT
            PaymentIntent paymentIntent = null;
            
            if ("STRIPE_CARD".equals(request.getPaymentMethod())) {
                if (request.getCardToken() != null && "tok_visa".equals(request.getCardToken())) {
                    // Use test card payment for "tok_visa"
                    paymentIntent = stripePaymentService.processTestCardPayment(
                        totalAmount,
                        "usd",
                        user.getEmail()
                    );
                } else if (request.getCardToken() != null) {
                    // Use regular payment method (for real payment method IDs)
                    paymentIntent = stripePaymentService.processCardPayment(
                        totalAmount,
                        "usd",
                        request.getCardToken(),
                        user.getEmail()
                    );
                } else if (request.getPaymentIntentId() != null) {
                    // Confirm existing payment intent
                    paymentIntent = stripePaymentService.confirmPaymentIntent(request.getPaymentIntentId());
                } else {
                    throw new RuntimeException("No Stripe payment method provided");
                }

                // Check if payment was successful
                if (!stripePaymentService.isPaymentSuccessful(paymentIntent)) {
                    throw new RuntimeException("Payment failed: " + paymentIntent.getStatus());
                }
                
                System.out.println("✅ Stripe payment successful! Payment Intent: " + paymentIntent.getId());
            }

            // CREATE ORDER AFTER SUCCESSFUL PAYMENT
            Order order = new Order();
            order.setUser(user);
            order.setOrderNumber(generateOrderNumber());
            order.setStatus(Order.OrderStatus.CONFIRMED); // Payment succeeded, so confirmed
            order.setTotalAmount(totalAmount);
            order.setPaymentMethod(request.getPaymentMethod());
            
            // 🔥 NEW: Save addresses and payment intent ID
            order.setShippingAddress(request.getShippingAddress());
            order.setBillingAddress(request.getBillingAddress());
            order.setStripePaymentIntentId(paymentIntent.getId());
            
            // Create order items and update stock
            for (CartItem cartItem : cart.getCartItems()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(cartItem.getProduct());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setUnitPrice(cartItem.getProduct().getPrice());
                orderItem.setTotalPrice(cartItem.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(cartItem.getQuantity())));
                
                order.getOrderItems().add(orderItem);
                
                // Update product stock
                Product product = cartItem.getProduct();
                product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
                productService.updateProduct(product.getId(), product);
            }

            // Save order and clear cart
            Order savedOrder = orderRepository.save(order);
            shoppingCartService.clearCart(userId);
            
            return DTOMapper.toOrderDTO(savedOrder);
            
        } catch (Exception e) {
            System.err.println("❌ Payment failed: " + e.getMessage());
            throw new RuntimeException("Order creation failed: " + e.getMessage());
        }
    }
    
    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    public List<OrderDTO> getUserOrders(Long userId) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);
        return orders.stream()
                .map(DTOMapper::toOrderDTO)
                .collect(Collectors.toList());
    }
    
    public Optional<OrderDTO> getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .map(DTOMapper::toOrderDTO);
    }
    
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(DTOMapper::toOrderDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, Order.OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        order.setStatus(newStatus);
        
        // Update relevant dates based on status
        switch (newStatus) {
            case SHIPPED:
                order.setShippedDate(LocalDateTime.now());
                break;
            case DELIVERED:
                order.setDeliveredDate(LocalDateTime.now());
                break;
            default:
                break;
        }
        
        Order savedOrder = orderRepository.save(order);
        return DTOMapper.toOrderDTO(savedOrder);
    }
}