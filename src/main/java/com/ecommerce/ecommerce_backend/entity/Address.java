package com.ecommerce.ecommerce_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "addresses")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "address_line_1", nullable = false, length = 255)
    private String addressLine1;

    @Column(name = "address_line_2", length = 255)
    private String addressLine2;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private AddressType type = AddressType.BOTH;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum AddressType {
        SHIPPING, BILLING, BOTH
    }

    // Lifecycle methods
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper method for formatted address
    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(addressLine1);
        if (addressLine2 != null && !addressLine2.trim().isEmpty()) {
            sb.append(", ").append(addressLine2);
        }
        sb.append(", ").append(city);
        sb.append(", ").append(state);
        sb.append(" ").append(postalCode);
        sb.append(", ").append(country);
        return sb.toString();
    }

    // Constructors
    public Address() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

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

    public AddressType getType() { return type; }
    public void setType(AddressType type) { this.type = type; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}