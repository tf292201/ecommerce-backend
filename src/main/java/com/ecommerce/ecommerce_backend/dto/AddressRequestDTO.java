// 1. AddressRequestDTO.java
package com.ecommerce.ecommerce_backend.dto;

import com.ecommerce.ecommerce_backend.entity.Address;

public class AddressRequestDTO {
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private Boolean isDefault = false;
    private Address.AddressType type = Address.AddressType.BOTH;

    // Constructors
    public AddressRequestDTO() {}

    public AddressRequestDTO(String addressLine1, String addressLine2, String city, 
                           String state, String postalCode, String country, 
                           Boolean isDefault, Address.AddressType type) {
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        this.isDefault = isDefault;
        this.type = type;
    }

    // Getters and Setters
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }

    public Address.AddressType getType() { return type; }
    public void setType(Address.AddressType type) { this.type = type; }
}