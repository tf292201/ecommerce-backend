package com.ecommerce.ecommerce_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateOrderRequestDTO {
    @NotNull(message = "Payment method is required")
    @NotBlank(message = "Payment method cannot be blank")
    private String paymentMethod;
    
    // Stripe payment fields
    private String cardToken;
    private String paymentIntentId;
    private String paymentMethodId;
    
    // Address options - either use existing addresses or provide new ones
    private Long shippingAddressId;  // Use existing saved address
    private Long billingAddressId;   // Use existing saved address
    
    // New address fields (if not using existing addresses)
    private AddressRequestDTO shippingAddress;
    private AddressRequestDTO billingAddress;
    
    // Option to save new addresses for future use
    private Boolean saveShippingAddress = false;
    private Boolean saveBillingAddress = false;
    
    // Legacy fields for backward compatibility
    private String shippingAddressText;
    private String billingAddressText;
    
    // Constructors
    public CreateOrderRequestDTO() {}
    
    // Getters and Setters
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getCardToken() { return cardToken; }
    public void setCardToken(String cardToken) { this.cardToken = cardToken; }
    
    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }
    
    public String getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(String paymentMethodId) { this.paymentMethodId = paymentMethodId; }
    
    public Long getShippingAddressId() { return shippingAddressId; }
    public void setShippingAddressId(Long shippingAddressId) { this.shippingAddressId = shippingAddressId; }
    
    public Long getBillingAddressId() { return billingAddressId; }
    public void setBillingAddressId(Long billingAddressId) { this.billingAddressId = billingAddressId; }
    
    public AddressRequestDTO getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(AddressRequestDTO shippingAddress) { this.shippingAddress = shippingAddress; }
    
    public AddressRequestDTO getBillingAddress() { return billingAddress; }
    public void setBillingAddress(AddressRequestDTO billingAddress) { this.billingAddress = billingAddress; }
    
    public Boolean getSaveShippingAddress() { return saveShippingAddress; }
    public void setSaveShippingAddress(Boolean saveShippingAddress) { this.saveShippingAddress = saveShippingAddress; }
    
    public Boolean getSaveBillingAddress() { return saveBillingAddress; }
    public void setSaveBillingAddress(Boolean saveBillingAddress) { this.saveBillingAddress = saveBillingAddress; }
    
    public String getShippingAddressText() { return shippingAddressText; }
    public void setShippingAddressText(String shippingAddressText) { this.shippingAddressText = shippingAddressText; }
    
    public String getBillingAddressText() { return billingAddressText; }
    public void setBillingAddressText(String billingAddressText) { this.billingAddressText = billingAddressText; }
}