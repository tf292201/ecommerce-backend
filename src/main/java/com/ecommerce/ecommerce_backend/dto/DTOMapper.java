package com.ecommerce.ecommerce_backend.dto;

import com.ecommerce.ecommerce_backend.entity.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;


public class DTOMapper {
    
    // User mappings
    public static UserResponseDTO toUserResponseDTO(User user) {
        if (user == null) return null;
        
        Long cartId = (user.getShoppingCart() != null) ? user.getShoppingCart().getId() : null;
        
        return new UserResponseDTO(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getPhoneNumber(),
            user.getRole().toString(),
            user.getActive(),
            user.getCreatedAt(),
            user.getUpdatedAt(),
            cartId
        );
    }
    
    public static User toUserEntity(UserCreateRequestDTO dto) {
        if (dto == null) return null;
        
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhoneNumber(dto.getPhoneNumber());
        return user;
    }
    
    // Product mappings
    public static ProductDTO toProductDTO(Product product) {
        if (product == null) return null;
        
        return new ProductDTO(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getStockQuantity(),
            product.getActive(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
    
    // Cart item mappings - FIXED
    public static CartItemDTO toCartItemDTO(CartItem cartItem) {
        if (cartItem == null) return null;
        
        // Calculate unit price and total price from product
        BigDecimal unitPrice = cartItem.getProduct().getPrice();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
        
        return new CartItemDTO(
            cartItem.getId(),
            cartItem.getProduct().getId(),
            cartItem.getProduct().getName(),
            cartItem.getProduct().getDescription(),
            cartItem.getQuantity(),
            unitPrice,  // Get from product
            totalPrice, // Calculate here
            cartItem.getCreatedAt()
        );
    }
    
    // Shopping cart mappings - FIXED
    public static ShoppingCartDTO toShoppingCartDTO(ShoppingCart cart) {
        if (cart == null) return null;
        
        List<CartItemDTO> cartItemDTOs = cart.getCartItems().stream()
            .map(DTOMapper::toCartItemDTO)
            .collect(Collectors.toList());
        
        // Calculate total amount and total items
        BigDecimal totalAmount = cart.getCartItems().stream()
            .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        Integer totalItems = cart.getCartItems().stream()
            .mapToInt(CartItem::getQuantity)
            .sum();
        
        return new ShoppingCartDTO(
            cart.getId(),
            cart.getUser().getId(),
            cart.getUser().getUsername(),
            cartItemDTOs,
            totalAmount,    // Calculate here
            totalItems,     // Calculate here  
            cart.getCreatedAt(),
            cart.getUpdatedAt()
        );
    }

    public static OrderDTO toOrderDTO(Order order) {
        if (order == null) return null;
        
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setUserId(order.getUser().getId());
        dto.setUsername(order.getUser().getUsername());
        dto.setOrderStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPaymentMethod(order.getPaymentMethod());
        
        // For now, set addresses to null since Address entity getters aren't fully implemented
        dto.setShippingAddress(order.getShippingAddress());
    dto.setBillingAddress(order.getBillingAddress());
    dto.setStripePaymentIntentId(order.getStripePaymentIntentId());
    
    dto.setCreatedAt(order.getCreatedAt());
    dto.setUpdatedAt(order.getUpdatedAt());
    dto.setShippedDate(order.getShippedDate());
    dto.setDeliveredDate(order.getDeliveredDate());
        
        // Map order items
        List<OrderItemDTO> orderItemDTOs = order.getOrderItems().stream()
            .map(DTOMapper::toOrderItemDTO)
            .collect(Collectors.toList());
        dto.setOrderItems(orderItemDTOs);
        
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        
        return dto;
    }
    
    public static OrderItemDTO toOrderItemDTO(OrderItem orderItem) {
        if (orderItem == null) return null;
        
        return new OrderItemDTO(
            orderItem.getId(),
            orderItem.getProduct().getId(),
            orderItem.getProduct().getName(),
            orderItem.getQuantity(),
            orderItem.getUnitPrice(),
            orderItem.getTotalPrice()
        );
    }
    
}