package com.ecommerce.ecommerce_backend.repository;

import com.ecommerce.ecommerce_backend.entity.Order;
import com.ecommerce.ecommerce_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // Existing method you probably already have
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    
    // NEW: Get all orders ordered by created date (for admin)
    List<Order> findAllByOrderByCreatedAtDesc();
    
    // NEW: Alternative method names in case you use different naming
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    List<Order> findAllOrdersByCreatedAtDesc();

    boolean existsByOrderNumber(String orderNumber);
}