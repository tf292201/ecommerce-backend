package com.ecommerce.ecommerce_backend.service;

import com.ecommerce.ecommerce_backend.dto.AddressRequestDTO;
import com.ecommerce.ecommerce_backend.dto.AddressResponseDTO;
import com.ecommerce.ecommerce_backend.dto.DTOMapper;
import com.ecommerce.ecommerce_backend.entity.Address;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.repository.AddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AddressService {
    
    @Autowired
    private AddressRepository addressRepository;
    
    // Get all addresses for a user
    public List<AddressResponseDTO> getUserAddresses(User user) {
        List<Address> addresses = addressRepository.findByUserOrderByIsDefaultDescCreatedAtDesc(user);
        return addresses.stream()
                .map(DTOMapper::toAddressResponseDTO)
                .collect(Collectors.toList());
    }
    
    // Get addresses by type
    public List<AddressResponseDTO> getUserAddressesByType(User user, Address.AddressType type) {
        List<Address> addresses = addressRepository.findByUserAndTypeOrderByIsDefaultDescCreatedAtDesc(user, type);
        return addresses.stream()
                .map(DTOMapper::toAddressResponseDTO)
                .collect(Collectors.toList());
    }
    
    // Get default address
    public Optional<AddressResponseDTO> getDefaultAddress(User user) {
        return addressRepository.findByUserAndIsDefaultTrue(user)
                .map(DTOMapper::toAddressResponseDTO);
    }
    
    // Get default address by type
    public Optional<AddressResponseDTO> getDefaultAddressByType(User user, Address.AddressType type) {
        return addressRepository.findByUserAndTypeAndIsDefaultTrue(user, type)
                .map(DTOMapper::toAddressResponseDTO);
    }
    
    // Create new address
    @Transactional
    public AddressResponseDTO createAddress(User user, AddressRequestDTO requestDTO) {
        Address address = new Address();
        address.setUser(user);
        address.setAddressLine1(requestDTO.getAddressLine1());
        address.setAddressLine2(requestDTO.getAddressLine2());
        address.setCity(requestDTO.getCity());
        address.setState(requestDTO.getState());
        address.setPostalCode(requestDTO.getPostalCode());
        address.setCountry(requestDTO.getCountry());
        address.setType(requestDTO.getType());
        address.setIsDefault(requestDTO.getIsDefault());
        
        // If this is set as default, clear other default flags
        if (Boolean.TRUE.equals(requestDTO.getIsDefault())) {
            clearDefaultFlags(user, requestDTO.getType());
        }
        
        Address savedAddress = addressRepository.save(address);
        return DTOMapper.toAddressResponseDTO(savedAddress);
    }
    
    // Update existing address
    @Transactional
    public AddressResponseDTO updateAddress(User user, Long addressId, AddressRequestDTO requestDTO) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        
        // Check if address belongs to user
        if (!address.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied to this address");
        }
        
        address.setAddressLine1(requestDTO.getAddressLine1());
        address.setAddressLine2(requestDTO.getAddressLine2());
        address.setCity(requestDTO.getCity());
        address.setState(requestDTO.getState());
        address.setPostalCode(requestDTO.getPostalCode());
        address.setCountry(requestDTO.getCountry());
        address.setType(requestDTO.getType());
        address.setIsDefault(requestDTO.getIsDefault());
        
        // If this is set as default, clear other default flags
        if (Boolean.TRUE.equals(requestDTO.getIsDefault())) {
            clearDefaultFlags(user, requestDTO.getType());
        }
        
        Address savedAddress = addressRepository.save(address);
        return DTOMapper.toAddressResponseDTO(savedAddress);
    }
    
    // Set address as default
    @Transactional
    public AddressResponseDTO setAsDefault(User user, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        
        // Check if address belongs to user
        if (!address.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied to this address");
        }
        
        // Clear other default flags for this type
        clearDefaultFlags(user, address.getType());
        
        // Set this address as default
        address.setIsDefault(true);
        Address savedAddress = addressRepository.save(address);
        
        return DTOMapper.toAddressResponseDTO(savedAddress);
    }
    
    // Delete address
    @Transactional
    public void deleteAddress(User user, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        
        // Check if address belongs to user
        if (!address.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied to this address");
        }
        
        addressRepository.delete(address);
    }
    
    // Helper method to clear default flags
    private void clearDefaultFlags(User user, Address.AddressType type) {
        if (type == Address.AddressType.BOTH) {
            addressRepository.clearAllDefaultFlagsByUser(user);
        } else {
            addressRepository.clearDefaultFlagByUserAndType(user, type);
        }
    }
    
    // Save address from checkout (creates if new, updates if existing)
    @Transactional
    public AddressResponseDTO saveAddressFromCheckout(User user, AddressRequestDTO requestDTO, boolean setAsDefault) {
        // Check if similar address already exists
        List<Address> existingAddresses = addressRepository.findByUserOrderByIsDefaultDescCreatedAtDesc(user);
        
        for (Address existing : existingAddresses) {
            if (isSameAddress(existing, requestDTO)) {
                // Update existing address if needed
                if (setAsDefault && !Boolean.TRUE.equals(existing.getIsDefault())) {
                    return setAsDefault(user, existing.getId());
                }
                return DTOMapper.toAddressResponseDTO(existing);
            }
        }
        
        // Create new address
        requestDTO.setIsDefault(setAsDefault);
        return createAddress(user, requestDTO);
    }
    
    // Helper method to check if addresses are the same
    private boolean isSameAddress(Address existing, AddressRequestDTO request) {
        return existing.getAddressLine1().trim().equalsIgnoreCase(request.getAddressLine1().trim()) &&
               existing.getCity().trim().equalsIgnoreCase(request.getCity().trim()) &&
               existing.getState().trim().equalsIgnoreCase(request.getState().trim()) &&
               existing.getPostalCode().trim().equalsIgnoreCase(request.getPostalCode().trim()) &&
               existing.getCountry().trim().equalsIgnoreCase(request.getCountry().trim()) &&
               (existing.getAddressLine2() == null ? request.getAddressLine2() == null : 
                existing.getAddressLine2().trim().equalsIgnoreCase(
                    request.getAddressLine2() != null ? request.getAddressLine2().trim() : ""));
    }
    
    // Get address by ID for user
    public Optional<AddressResponseDTO> getAddressById(User user, Long addressId) {
        return addressRepository.findById(addressId)
                .filter(address -> address.getUser().getId().equals(user.getId()))
                .map(DTOMapper::toAddressResponseDTO);
    }
}