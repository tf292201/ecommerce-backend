package com.ecommerce.ecommerce_backend.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.ecommerce_backend.dto.CreateOrderRequestDTO;
import com.ecommerce.ecommerce_backend.dto.OrderDTO;
import com.ecommerce.ecommerce_backend.entity.Order;
import com.ecommerce.ecommerce_backend.entity.OrderItem;
import com.ecommerce.ecommerce_backend.entity.CartItem;  // ✅ YOUR ACTUAL ENTITY
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.entity.Address;
import com.ecommerce.ecommerce_backend.entity.ShoppingCart;  // ✅ YOUR ACTUAL ENTITY
import com.ecommerce.ecommerce_backend.repository.OrderRepository;
import com.ecommerce.ecommerce_backend.dto.DTOMapper;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private StripePaymentService stripePaymentService;  // ✅ FIXED: Use your actual payment service

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressService addressService;

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    public OrderDTO createOrderFromCart(String userEmail, CreateOrderRequestDTO request) {
        logger.info("🛒 Creating order for user: {}", userEmail);

        try {
            // Get user and cart
            User user = userService.getUserByUsername(userEmail)  // ✅ FIXED: Use your actual method
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // ✅ FIXED: Get cart from user
            ShoppingCart cart = user.getShoppingCart();
            if (cart == null || cart.getCartItems().isEmpty()) {
                throw new RuntimeException("Cannot create order: cart is empty");
            }

            List<CartItem> cartItems = cart.getCartItems();  // ✅ FIXED: Use your actual entity

            // Calculate total
            BigDecimal totalAmount = cartItems.stream()
                    .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            logger.info("💰 Order total: ${}", totalAmount);

            // Create inventory map for reservation
            Map<Long, Integer> inventoryMap = cartItems.stream()
                    .collect(Collectors.toMap(
                            item -> item.getProduct().getId(),
                            CartItem::getQuantity,  // ✅ FIXED: Use CartItem
                            Integer::sum
                    ));

            // Reserve inventory atomically
            logger.info("🔒 Attempting to reserve inventory...");
            if (!inventoryService.reserveInventory(inventoryMap)) {
                throw new RuntimeException("Insufficient stock for one or more items in your cart");
            }

            try {
                // Process payment using your StripePaymentService
                logger.info("💳 Processing payment...");
                
                boolean paymentSuccessful = false;
                String paymentIntentId = null;
                
                if ("STRIPE_CARD".equals(request.getPaymentMethod())) {
                    try {
                        com.stripe.model.PaymentIntent paymentIntent;
                        
                        if (request.getCardToken() != null) {
                            if ("tok_visa".equals(request.getCardToken())) {
                                // Use test payment method
                                paymentIntent = stripePaymentService.processTestCardPayment(
                                    totalAmount, "usd", user.getEmail());
                            } else {
                                // Use real card token
                                paymentIntent = stripePaymentService.processCardPayment(
                                    totalAmount, "usd", request.getCardToken(), user.getEmail());
                            }
                        } else if (request.getPaymentIntentId() != null) {
                            // Confirm existing payment intent
                            paymentIntent = stripePaymentService.confirmPaymentIntent(request.getPaymentIntentId());
                        } else {
                            throw new RuntimeException("No payment method provided");
                        }
                        
                        paymentSuccessful = stripePaymentService.isPaymentSuccessful(paymentIntent);
                        paymentIntentId = paymentIntent.getId();
                        
                        if (!paymentSuccessful) {
                            throw new RuntimeException("Payment failed: " + paymentIntent.getStatus());
                        }
                        
                        logger.info("✅ Payment successful! Payment Intent: {}", paymentIntentId);
                        
                    } catch (Exception e) {
                        logger.error("❌ Payment processing failed: {}", e.getMessage());
                        throw new RuntimeException("Payment failed: " + e.getMessage());
                    }
                } else {
                    // For other payment methods, mark as successful for testing
                    paymentSuccessful = true;
                    logger.info("✅ Non-Stripe payment method accepted");
                }

                // Create the order
                Order order = new Order();
                order.setUser(user);
                order.setTotalAmount(totalAmount);
                order.setStatus(Order.OrderStatus.CONFIRMED);  // ✅ FIXED: Use correct method
                order.setPaymentMethod(request.getPaymentMethod());
                if (paymentIntentId != null) {
                    order.setStripePaymentIntentId(paymentIntentId);
                }

                // Handle addresses - simplified version
                String finalShippingAddress = getShippingAddress(user, request);
                String finalBillingAddress = getBillingAddress(user, request);
                
                order.setShippingAddress(finalShippingAddress);
                order.setBillingAddress(finalBillingAddress);

                // Save the order first
                final Order savedOrder = orderRepository.save(order);
                logger.info("✅ Order created with ID: {}", savedOrder.getId());

                // Create order items
                List<OrderItem> orderItems = cartItems.stream().map(cartItem -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(savedOrder);
                    orderItem.setProduct(cartItem.getProduct());
                    orderItem.setQuantity(cartItem.getQuantity());
                    orderItem.setUnitPrice(cartItem.getProduct().getPrice());  // ✅ Use unit price from product
                    return orderItem;
                }).collect(Collectors.toList());

                savedOrder.setOrderItems(orderItems);
                orderRepository.save(savedOrder);

                // Try to save addresses if requested (optional - won't break if it fails)
                try {
                    handleAddressSaving(user, request, finalShippingAddress, finalBillingAddress);
                } catch (Exception e) {
                    logger.warn("⚠️ Address saving failed but order was successful: {}", e.getMessage());
                }

                // Clear the cart using your existing method
                shoppingCartService.clearCart(user.getId());  // ✅ FIXED: Use actual method
                logger.info("🧹 Cart cleared for user: {}", userEmail);

                return DTOMapper.toOrderDTO(savedOrder);

            } catch (Exception e) {
                // Release inventory if payment or order creation fails
                logger.error("❌ Order creation failed, releasing inventory: {}", e.getMessage());
                inventoryService.releaseInventory(inventoryMap);
                throw e;
            }

        } catch (Exception e) {
            logger.error("❌ Order creation failed for user {}: {}", userEmail, e.getMessage());
            throw new RuntimeException("Failed to create order: " + e.getMessage());
        }
    }

    /**
     * Get shipping address - simplified version
     */
    private String getShippingAddress(User user, CreateOrderRequestDTO request) {
        // Option 1: Use existing saved address ID
        if (request.getShippingAddressId() != null) {  // ✅ FIXED: Use your actual method
            Optional<Address> existingAddress = addressService.getAddressById(request.getShippingAddressId());
            if (existingAddress.isPresent() && existingAddress.get().getUser().getId().equals(user.getId())) {
                return formatAddressToString(existingAddress.get());
            }
        }

        // Option 2: Use simple address string (backwards compatibility)
        if (request.getShippingAddress() != null && !request.getShippingAddress().trim().isEmpty()) {
            return request.getShippingAddress();
        }

        // Option 3: Try to get user's default shipping address
        Optional<Address> defaultAddress = addressService.getDefaultAddress(user.getId(), "SHIPPING");
        if (defaultAddress.isPresent()) {
            return formatAddressToString(defaultAddress.get());
        }

        throw new RuntimeException("No shipping address provided or found");
    }

    /**
     * Get billing address - simplified version
     */
    private String getBillingAddress(User user, CreateOrderRequestDTO request) {
        // Option 1: Use existing saved address ID
        if (request.getBillingAddressId() != null) {  // ✅ FIXED: Use your actual method
            Optional<Address> existingAddress = addressService.getAddressById(request.getBillingAddressId());
            if (existingAddress.isPresent() && existingAddress.get().getUser().getId().equals(user.getId())) {
                return formatAddressToString(existingAddress.get());
            }
        }

        // Option 2: Use simple address string
        if (request.getBillingAddress() != null && !request.getBillingAddress().trim().isEmpty()) {
            return request.getBillingAddress();
        }

        // Option 3: Use shipping address as billing address
        return getShippingAddress(user, request);
    }

    /**
     * Handle saving addresses - optional feature
     */
    private void handleAddressSaving(User user, CreateOrderRequestDTO request, 
                                   String finalShippingAddress, String finalBillingAddress) {
        
        logger.info("💾 Processing address saving preferences for user: {}", user.getEmail());

        // Since your DTO doesn't have save flags, this is just logging for now
        // You can add getSaveShippingAddress() and getSaveBillingAddress() methods to your DTO later
        logger.info("🏠 Address saving feature available for future implementation");
    }

    /**
     * Format address object to string for order storage
     */
    private String formatAddressToString(Address address) {
        StringBuilder addressStr = new StringBuilder();
        addressStr.append(address.getAddressLine1());
        
        if (address.getAddressLine2() != null && !address.getAddressLine2().trim().isEmpty()) {
            addressStr.append(", ").append(address.getAddressLine2());
        }
        
        addressStr.append(", ").append(address.getCity());
        addressStr.append(", ").append(address.getState());
        addressStr.append(" ").append(address.getPostalCode());
        addressStr.append(", ").append(address.getCountry());
        
        return addressStr.toString();
    }

    // Keep your existing methods
    public List<OrderDTO> getUserOrders(String userEmail) {
        User user = userService.getUserByUsername(userEmail)  // ✅ FIXED: Use your actual method
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);  // ✅ FIXED: Use actual method
        return orders.stream()
                .map(DTOMapper::toOrderDTO)
                .collect(Collectors.toList());
    }

    public OrderDTO getOrderById(String userEmail, Long orderId) {
        User user = userService.getUserByUsername(userEmail)  // ✅ FIXED: Use your actual method
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ✅ NEED TO ADD THIS METHOD TO YOUR OrderRepository
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Security check - make sure order belongs to user
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied to this order");
        }

        return DTOMapper.toOrderDTO(order);
    }
}