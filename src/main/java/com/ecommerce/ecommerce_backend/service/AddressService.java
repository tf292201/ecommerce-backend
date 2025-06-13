package com.ecommerce.ecommerce_backend.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ecommerce.ecommerce_backend.entity.Address;
import com.ecommerce.ecommerce_backend.entity.Address.AddressType;
import com.ecommerce.ecommerce_backend.repository.AddressRepository;
import com.ecommerce.ecommerce_backend.dto.AddressDTO;

@Service
@Transactional
public class AddressService {

    @Autowired
    private AddressRepository addressRepository;

    private static final Logger logger = LoggerFactory.getLogger(AddressService.class);

    /**
     * Get all addresses for a user
     */
    public List<Address> getAddressesByUserId(Long userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
    }

    /**
     * Get address by ID
     */
    public Optional<Address> getAddressById(Long addressId) {
        return addressRepository.findById(addressId);
    }

    /**
     * Save an address
     */
    public Address saveAddress(Address address) {
        return addressRepository.save(address);
    }

    /**
     * Delete an address
     */
    public void deleteAddress(Long addressId) {
        addressRepository.deleteById(addressId);
    }

    /**
     * Unset default addresses of a specific type for a user
     */
    public void unsetDefaultAddresses(Long userId, String addressTypeString) {
        AddressType addressType = parseAddressType(addressTypeString);
        List<Address> defaultAddresses = addressRepository.findByUserIdAndTypeAndIsDefaultTrue(userId, addressType);
        defaultAddresses.forEach(address -> {
            address.setIsDefault(false);
            addressRepository.save(address);
        });
        logger.info("🔄 Unset {} default {} addresses for user ID: {}", defaultAddresses.size(), addressTypeString.toLowerCase(), userId);
    }

    /**
     * Get default address for a user and type
     */
    public Optional<Address> getDefaultAddress(Long userId, String addressTypeString) {
        AddressType addressType = parseAddressType(addressTypeString);
        return addressRepository.findByUserIdAndTypeAndIsDefaultTrue(userId, addressType)
                .stream()
                .findFirst();
    }

    /**
     * Check if an address already exists for a user (to avoid duplicates)
     */
    public boolean addressExistsForUser(Long userId, AddressDTO addressDTO, String addressTypeString) {
        AddressType addressType = parseAddressType(addressTypeString);
        List<Address> existingAddresses = addressRepository.findByUserIdAndType(userId, addressType);
        
        return existingAddresses.stream().anyMatch(existing -> 
            existing.getAddressLine1().equalsIgnoreCase(addressDTO.getStreet()) &&
            existing.getCity().equalsIgnoreCase(addressDTO.getCity()) &&
            existing.getState().equalsIgnoreCase(addressDTO.getState()) &&
            existing.getPostalCode().equalsIgnoreCase(addressDTO.getZipCode())
        );
    }

    /**
     * Get user's addresses by type
     */
    public List<Address> getAddressesByUserIdAndType(Long userId, String addressTypeString) {
        AddressType addressType = parseAddressType(addressTypeString);
        return addressRepository.findByUserIdAndType(userId, addressType);
    }

    /**
     * Count addresses for a user
     */
    public long countAddressesForUser(Long userId) {
        return addressRepository.countByUserId(userId);
    }

    /**
     * Get the most recently used address of a type
     */
    public Optional<Address> getMostRecentAddress(Long userId, String addressTypeString) {
        AddressType addressType = parseAddressType(addressTypeString);
        List<Address> addresses = addressRepository.findByUserIdAndTypeOrderByUpdatedAtDesc(userId, addressType);
        return addresses.isEmpty() ? Optional.empty() : Optional.of(addresses.get(0));
    }

    /**
     * Helper method to parse address type string to enum
     */
    private AddressType parseAddressType(String addressTypeString) {
        if (addressTypeString == null) {
            return AddressType.BOTH;
        }
        
        switch (addressTypeString.toUpperCase()) {
            case "SHIPPING":
                return AddressType.SHIPPING;
            case "BILLING":
                return AddressType.BILLING;
            case "BOTH":
            default:
                return AddressType.BOTH;
        }
    }
}