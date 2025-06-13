package com.ecommerce.ecommerce_backend.dto;

import java.time.LocalDateTime;
import com.ecommerce.ecommerce_backend.entity.Address.AddressType;

public class AddressDTO {
    private Long id;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private AddressType type;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Default constructor
    public AddressDTO() {}

    // Constructor with essential fields
    public AddressDTO(String addressLine1, String city, String state, String postalCode, String country) {
        this.addressLine1 = addressLine1;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }

    // Full constructor
    public AddressDTO(Long id, String addressLine1, String addressLine2, String city, String state, 
                     String postalCode, String country, AddressType type, Boolean isDefault, 
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        this.type = type;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public AddressType getType() {
        return type;
    }

    public void setType(AddressType type) {
        this.type = type;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods for backwards compatibility
    public String getStreet() {
        return addressLine1 + (addressLine2 != null && !addressLine2.isEmpty() ? ", " + addressLine2 : "");
    }

    public String getZipCode() {
        return postalCode;
    }

    public String getAddressType() {
        return type != null ? type.toString() : "BOTH";
    }

    // Utility methods
    public String getFormattedAddress() {
        String line2 = addressLine2 != null && !addressLine2.isEmpty() ? ", " + addressLine2 : "";
        return String.format("%s%s, %s, %s %s, %s", 
                addressLine1, line2, city, state, postalCode, country);
    }

    public boolean isShippingAddress() {
        return type == AddressType.SHIPPING || type == AddressType.BOTH;
    }

    public boolean isBillingAddress() {
        return type == AddressType.BILLING || type == AddressType.BOTH;
    }

    @Override
    public String toString() {
        return "AddressDTO{" +
                "id=" + id +
                ", addressLine1='" + addressLine1 + '\'' +
                ", addressLine2='" + addressLine2 + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", country='" + country + '\'' +
                ", type=" + type +
                ", isDefault=" + isDefault +
                '}';
    }
}