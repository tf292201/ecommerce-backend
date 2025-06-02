package com.ecommerce.ecommerce_backend.entity;

import jakarta.persistence.*;

// Address.java
@Entity
@Table(name = "addresses")
public class Address {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "address_line_1", nullable = false)
    private String addressLine1;
    
    @Column(name = "address_line_2")
    private String addressLine2;
    
    @Column(nullable = false)
    private String city;
    
    @Column(nullable = false)
    private String state;
    
    @Column(name = "postal_code", nullable = false)
    private String postalCode;
    
    @Column(nullable = false)
    private String country;
    
    @Column(name = "is_default")
    private Boolean isDefault = false;
    
    @Enumerated(EnumType.STRING)
    private AddressType type = AddressType.BOTH;
    
    // Constructors, getters, setters
    public Address() {}
    
    // Getters and Setters (follow same pattern)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    // ... other getters and setters
    
    public enum AddressType {
        BILLING, SHIPPING, BOTH
    }
}