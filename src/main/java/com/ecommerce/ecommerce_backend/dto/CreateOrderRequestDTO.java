package com.ecommerce.ecommerce_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateOrderRequestDTO {
    @NotNull(message = "Payment method is required")
    @NotBlank(message = "Payment method cannot be blank")
    private String paymentMethod;
    
    // Stripe payment fields
    private String cardToken; // Stripe card token from frontend (e.g., "tok_visa" for testing)
    private String paymentIntentId; // If using Stripe Payment Intents flow
    private String paymentMethodId; // Stripe Payment Method ID (newer approach)
    
    private Long shippingAddressId;
    private Long billingAddressId;
    
    // For simplified address (if no address IDs provided)
    private String shippingAddress;
    private String billingAddress;
    
    // Constructors
    public CreateOrderRequestDTO() {}
    
    // Getters and Setters
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    // Stripe payment getters/setters
    public String getCardToken() { return cardToken; }
    public void setCardToken(String cardToken) { this.cardToken = cardToken; }
    
    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }
    
    public String getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(String paymentMethodId) { this.paymentMethodId = paymentMethodId; }
    
    // Address getters/setters
    public Long getShippingAddressId() { return shippingAddressId; }
    public void setShippingAddressId(Long shippingAddressId) { this.shippingAddressId = shippingAddressId; }
    
    public Long getBillingAddressId() { return billingAddressId; }
    public void setBillingAddressId(Long billingAddressId) { this.billingAddressId = billingAddressId; }
    
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    
    public String getBillingAddress() { return billingAddress; }
    public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }
}