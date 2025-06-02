package com.ecommerce.ecommerce_backend.repository;

import com.ecommerce.ecommerce_backend.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    // Find all cart items for a specific shopping cart
    List<CartItem> findByShoppingCartId(Long shoppingCartId);
    
    // Find cart item by shopping cart and product
    @Query("SELECT ci FROM CartItem ci WHERE ci.shoppingCart.id = :cartId AND ci.product.id = :productId")
    Optional<CartItem> findByShoppingCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);
    
    // Find all cart items for a specific product
    List<CartItem> findByProductId(Long productId);
    
    // Delete all cart items for a specific shopping cart
    // 🔧 FIXED: Added @Modifying and @Transactional
    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem ci WHERE ci.shoppingCart.id = :cartId")
    void deleteByShoppingCartId(@Param("cartId") Long cartId);
    
    // Count total items in a cart
    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.shoppingCart.id = :cartId")
    Integer countByShoppingCartId(@Param("cartId") Long cartId);
    
    // Get total quantity of items in a cart
    @Query("SELECT COALESCE(SUM(ci.quantity), 0) FROM CartItem ci WHERE ci.shoppingCart.id = :cartId")
    Integer getTotalQuantityByShoppingCartId(@Param("cartId") Long cartId);
}