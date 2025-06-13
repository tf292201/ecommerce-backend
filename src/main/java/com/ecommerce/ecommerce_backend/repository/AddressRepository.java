package com.ecommerce.ecommerce_backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.ecommerce.ecommerce_backend.entity.Address;
import com.ecommerce.ecommerce_backend.entity.Address.AddressType;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    /**
     * Find all addresses for a user, ordered by default first, then by creation date
     */
    List<Address> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);

    /**
     * Find addresses by user and type (using enum)
     */
    List<Address> findByUserIdAndType(Long userId, AddressType type);

    /**
     * Find default addresses by user and type (using enum)
     */
    List<Address> findByUserIdAndTypeAndIsDefaultTrue(Long userId, AddressType type);

    /**
     * Find addresses by user and type, ordered by update date
     */
    List<Address> findByUserIdAndTypeOrderByUpdatedAtDesc(Long userId, AddressType type);

    /**
     * Count addresses for a user
     */
    long countByUserId(Long userId);

    /**
     * Delete all addresses for a user (useful for user deletion)
     */
    void deleteByUserId(Long userId);

    /**
     * Check if user has any default address of a type (using enum)
     */
    boolean existsByUserIdAndTypeAndIsDefaultTrue(Long userId, AddressType type);
}