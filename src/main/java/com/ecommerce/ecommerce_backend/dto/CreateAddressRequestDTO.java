package com.ecommerce.ecommerce_backend.dto;

public class CreateAddressRequestDTO {
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;  
    private String postalCode;
    private String country;
    private String addressType; // "SHIPPING", "BILLING", or "BOTH"
    private Boolean isDefault = false;
    
    // Constructors
    public CreateAddressRequestDTO() {}
    
    // Getters and setters
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
    
    public String getAddressType() { 
        return addressType; 
    }
    
    public void setAddressType(String addressType) { 
        this.addressType = addressType; 
    }
    
    public Boolean getIsDefault() { 
        return isDefault; 
    }
    
    public void setIsDefault(Boolean isDefault) { 
        this.isDefault = isDefault; 
    }
}