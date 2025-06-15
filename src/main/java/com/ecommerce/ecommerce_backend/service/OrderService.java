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

import com.ecommerce.ecommerce_backend.dto.AddressRequestDTO;
import com.ecommerce.ecommerce_backend.dto.CreateOrderRequestDTO;
import com.ecommerce.ecommerce_backend.dto.OrderDTO;
import com.ecommerce.ecommerce_backend.entity.Order;
import com.ecommerce.ecommerce_backend.entity.OrderItem;
import com.ecommerce.ecommerce_backend.entity.CartItem;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.entity.Address;
import com.ecommerce.ecommerce_backend.entity.ShoppingCart;
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
    private StripePaymentService stripePaymentService;

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
            User user = userService.getUserByUsername(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get cart from user
            ShoppingCart cart = user.getShoppingCart();
            if (cart == null || cart.getCartItems().isEmpty()) {
                throw new RuntimeException("Cannot create order: cart is empty");
            }

            List<CartItem> cartItems = cart.getCartItems();

            // Calculate total
            BigDecimal totalAmount = cartItems.stream()
                    .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            logger.info("💰 Order total: ${}", totalAmount);

            // Create inventory map for reservation
            Map<Long, Integer> inventoryMap = cartItems.stream()
                    .collect(Collectors.toMap(
                            item -> item.getProduct().getId(),
                            CartItem::getQuantity,
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

                // Generate unique order number
                String orderNumber = generateOrderNumber();
                logger.info("📋 Generated order number: {}", orderNumber);

                // Create the order
                Order order = new Order();
                order.setUser(user);
                order.setTotalAmount(totalAmount);
                order.setStatus(Order.OrderStatus.CONFIRMED);
                order.setPaymentMethod(request.getPaymentMethod());
                order.setOrderNumber(orderNumber); // ✅ FIXED: Set order number
                if (paymentIntentId != null) {
                    order.setStripePaymentIntentId(paymentIntentId);
                }

                // Handle addresses - simplified version that works with your current service
                String finalShippingAddress = getShippingAddress(user, request);
                String finalBillingAddress = getBillingAddress(user, request);
                
                order.setShippingAddress(finalShippingAddress);
                order.setBillingAddress(finalBillingAddress);

                // Save the order first
                final Order savedOrder = orderRepository.save(order);
                logger.info("✅ Order created with ID: {} and Order Number: {}", savedOrder.getId(), savedOrder.getOrderNumber());

                // Create order items
                List<OrderItem> orderItems = cartItems.stream().map(cartItem -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(savedOrder);
                    orderItem.setProduct(cartItem.getProduct());
                    orderItem.setQuantity(cartItem.getQuantity());
                    orderItem.setUnitPrice(cartItem.getProduct().getPrice());
                    return orderItem;
                }).collect(Collectors.toList());

                savedOrder.setOrderItems(orderItems);
                orderRepository.save(savedOrder);

                // Try to save addresses for future use (optional feature)
                try {
                    saveAddressesFromOrder(user, request);
                } catch (Exception e) {
                    logger.warn("⚠️ Address saving failed but order was successful: {}", e.getMessage());
                }

                // Clear the cart using your existing method
                shoppingCartService.clearCart(user.getId());
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
     * Generate unique order number
     */
    private String generateOrderNumber() {
        // Generate order number in format: ORD-YYYYMMDD-XXXXXX
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String datePart = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // Generate random 6-digit number
        int randomPart = (int) (Math.random() * 900000) + 100000;
        
        String orderNumber = "ORD-" + datePart + "-" + randomPart;
        
        // Check if order number already exists, regenerate if it does
        int attempts = 0;
        while (attempts < 10 && orderRepository.existsByOrderNumber(orderNumber)) {
            randomPart = (int) (Math.random() * 900000) + 100000;
            orderNumber = "ORD-" + datePart + "-" + randomPart;
            attempts++;
        }
        
        logger.info("📋 Generated order number: {}", orderNumber);
        return orderNumber;
    }

    /**
     * Get shipping address - NOW FULLY FUNCTIONAL with your AddressService
     */
    private String getShippingAddress(User user, CreateOrderRequestDTO request) {
        // Option 1: Use address object from request (your current case)
        if (request.getShippingAddress() != null) {
            AddressRequestDTO shippingAddr = request.getShippingAddress();
            return formatAddressRequestToString(shippingAddr);
        }

        // Option 2: Use existing saved address ID
        if (request.getShippingAddressId() != null) {
            try {
                Optional<Address> existingAddress = addressService.getAddressByUserAndId(user, request.getShippingAddressId());
                if (existingAddress.isPresent()) {
                    return formatAddressToString(existingAddress.get());
                }
            } catch (Exception e) {
                logger.warn("⚠️ Could not find saved address with ID: {}", request.getShippingAddressId());
            }
        }

        // Option 3: Try to get default shipping address
        try {
            Optional<Address> defaultShippingAddress = addressService.getDefaultAddressEntityByType(user, Address.AddressType.SHIPPING);
            if (defaultShippingAddress.isPresent()) {
                logger.info("📍 Using default shipping address for user: {}", user.getUsername());
                return formatAddressToString(defaultShippingAddress.get());
            }
            
            // Fallback to any default address
            Optional<Address> anyDefaultAddress = addressService.getDefaultAddressEntity(user);
            if (anyDefaultAddress.isPresent()) {
                logger.info("📍 Using default address as shipping address for user: {}", user.getUsername());
                return formatAddressToString(anyDefaultAddress.get());
            }
        } catch (Exception e) {
            logger.warn("⚠️ Could not find default address for user: {}", user.getUsername());
        }

        throw new RuntimeException("No shipping address provided and no default address found");
    }

    /**
     * Get billing address - NOW FULLY FUNCTIONAL with your AddressService
     */
    private String getBillingAddress(User user, CreateOrderRequestDTO request) {
        // Option 1: Use address object from request
        if (request.getBillingAddress() != null) {
            AddressRequestDTO billingAddr = request.getBillingAddress();
            return formatAddressRequestToString(billingAddr);
        }

        // Option 2: Use existing saved address ID
        if (request.getBillingAddressId() != null) {
            try {
                Optional<Address> existingAddress = addressService.getAddressByUserAndId(user, request.getBillingAddressId());
                if (existingAddress.isPresent()) {
                    return formatAddressToString(existingAddress.get());
                }
            } catch (Exception e) {
                logger.warn("⚠️ Could not find saved billing address with ID: {}", request.getBillingAddressId());
            }
        }

        // Option 3: Try to get default billing address
        try {
            Optional<Address> defaultBillingAddress = addressService.getDefaultAddressEntityByType(user, Address.AddressType.BILLING);
            if (defaultBillingAddress.isPresent()) {
                logger.info("📍 Using default billing address for user: {}", user.getUsername());
                return formatAddressToString(defaultBillingAddress.get());
            }
        } catch (Exception e) {
            logger.warn("⚠️ Could not find default billing address for user: {}", user.getUsername());
        }

        // Option 4: Use shipping address as billing address (common fallback)
        logger.info("📍 Using shipping address as billing address");
        return getShippingAddress(user, request);
    }

    /**
     * Format AddressRequestDTO to string for order storage - FIXED null safety
     */
    private String formatAddressRequestToString(AddressRequestDTO address) {
        StringBuilder addressStr = new StringBuilder();
        
        if (address.getAddressLine1() != null) {
            addressStr.append(address.getAddressLine1().trim());
        }
        
        if (address.getAddressLine2() != null && !address.getAddressLine2().trim().isEmpty()) {
            addressStr.append(", ").append(address.getAddressLine2().trim());
        }
        
        if (address.getCity() != null) {
            addressStr.append(", ").append(address.getCity().trim());
        }
        
        if (address.getState() != null) {
            addressStr.append(", ").append(address.getState().trim());
        }
        
        if (address.getPostalCode() != null) {
            addressStr.append(" ").append(address.getPostalCode().trim());
        }
        
        if (address.getCountry() != null) {
            addressStr.append(", ").append(address.getCountry().trim());
        }
        
        return addressStr.toString();
    }

    /**
     * Save addresses from order for future use (optional feature)
     */
    private void saveAddressesFromOrder(User user, CreateOrderRequestDTO request) {
        logger.info("💾 Processing address saving for user: {}", user.getUsername());

        // Save shipping address if provided and not already saved
        if (request.getShippingAddress() != null) {
            try {
                AddressRequestDTO shippingAddr = request.getShippingAddress();
                shippingAddr.setType(Address.AddressType.SHIPPING);
                addressService.saveAddressFromCheckout(user, shippingAddr, false);
                logger.info("✅ Shipping address saved for future use");
            } catch (Exception e) {
                logger.warn("⚠️ Failed to save shipping address: {}", e.getMessage());
            }
        }

        // Save billing address if provided and different from shipping
        if (request.getBillingAddress() != null) {
            try {
                AddressRequestDTO billingAddr = request.getBillingAddress();
                billingAddr.setType(Address.AddressType.BILLING);
                addressService.saveAddressFromCheckout(user, billingAddr, false);
                logger.info("✅ Billing address saved for future use");
            } catch (Exception e) {
                logger.warn("⚠️ Failed to save billing address: {}", e.getMessage());
            }
        }
    }

    /**
     * Format Address entity to string for order storage - For future use
     */
    private String formatAddressToString(Address address) {
        StringBuilder addressStr = new StringBuilder();
        
        if (address.getAddressLine1() != null) {
            addressStr.append(address.getAddressLine1().trim());
        }
        
        if (address.getAddressLine2() != null && !address.getAddressLine2().trim().isEmpty()) {
            addressStr.append(", ").append(address.getAddressLine2().trim());
        }
        
        if (address.getCity() != null) {
            addressStr.append(", ").append(address.getCity().trim());
        }
        
        if (address.getState() != null) {
            addressStr.append(", ").append(address.getState().trim());
        }
        
        if (address.getPostalCode() != null) {
            addressStr.append(" ").append(address.getPostalCode().trim());
        }
        
        if (address.getCountry() != null) {
            addressStr.append(", ").append(address.getCountry().trim());
        }
        
        return addressStr.toString();
    }

    // FIXED: Methods to match your OrderController expectations
    
    /**
     * Get user orders by user ID (for controller)
     */
    public List<OrderDTO> getUserOrders(Long userId) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);
        return orders.stream()
                .map(DTOMapper::toOrderDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get user orders by username (alternative method)
     */
    public List<OrderDTO> getUserOrdersByUsername(String userEmail) {
        User user = userService.getUserByUsername(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);
        return orders.stream()
                .map(DTOMapper::toOrderDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get order by ID only (for admin - no user check)
     */
    public Optional<OrderDTO> getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .map(DTOMapper::toOrderDTO);
    }

    /**
     * Get order by ID with user verification (for user endpoints)
     */
    public OrderDTO getOrderByIdForUser(String userEmail, Long orderId) {
        User user = userService.getUserByUsername(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Security check - make sure order belongs to user
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied to this order");
        }

        return DTOMapper.toOrderDTO(order);
    }

    /**
     * Get all orders (for admin)
     */
    public List<OrderDTO> getAllOrders() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        return orders.stream()
                .map(DTOMapper::toOrderDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update order status (for admin)
     */
    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        order.setStatus(status);
        Order savedOrder = orderRepository.save(order);
        
        logger.info("✅ Order {} status updated to: {}", orderId, status);
        return DTOMapper.toOrderDTO(savedOrder);
    }
}