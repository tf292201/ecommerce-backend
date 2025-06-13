package com.ecommerce.ecommerce_backend.repository;

import com.ecommerce.ecommerce_backend.entity.Address;
import com.ecommerce.ecommerce_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    
    List<Address> findByUserOrderByIsDefaultDescCreatedAtDesc(User user);
    
    List<Address> findByUserAndTypeOrderByIsDefaultDescCreatedAtDesc(User user, Address.AddressType type);
    
    Optional<Address> findByUserAndIsDefaultTrue(User user);
    
    Optional<Address> findByIdAndUser(Long id, User user);
    
    // Method names that match what your AddressService is calling
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user = :user")
    void clearAllDefaultFlagsByUser(@Param("user") User user);
    
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user = :user AND a.type = :type")
    void clearDefaultFlagByUserAndType(@Param("user") User user, @Param("type") Address.AddressType type);
    
    // Alternative method names (in case your service uses these)
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user = :user")
    void clearAllDefaultForUser(@Param("user") User user);
    
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user = :user AND a.type = :type")
    void clearDefaultForUserAndType(@Param("user") User user, @Param("type") Address.AddressType type);
}