package com.ecommerce.ecommerce_backend.service;

import com.ecommerce.ecommerce_backend.entity.Product;
import com.ecommerce.ecommerce_backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class InventoryService {

    @Autowired
    private ProductRepository productRepository;

    /**
     * Reserve inventory for multiple products atomically
     * Returns true if all reservations successful, false otherwise
     */
    @Transactional
    public boolean reserveInventory(Map<Long, Integer> productQuantities) {
        System.out.println("🔒 Attempting to reserve inventory...");
        
        // First, check if all products have sufficient stock
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer requestedQuantity = entry.getValue();
            
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
            
            System.out.println("📦 Product: " + product.getName() + 
                             " | Available: " + product.getStockQuantity() + 
                             " | Requested: " + requestedQuantity);
            
            if (!product.getActive()) {
                System.out.println("❌ Product " + product.getName() + " is not active");
                return false;
            }
            
            if (product.getStockQuantity() < requestedQuantity) {
                System.out.println("❌ Insufficient stock for " + product.getName() + 
                                 " (Available: " + product.getStockQuantity() + 
                                 ", Requested: " + requestedQuantity + ")");
                return false; // Insufficient stock
            }
        }
        
        // If all checks pass, reserve the inventory
        System.out.println("✅ All stock checks passed, reserving inventory...");
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer requestedQuantity = entry.getValue();
            
            Product product = productRepository.findById(productId).get();
            int newStock = product.getStockQuantity() - requestedQuantity;
            product.setStockQuantity(newStock);
            productRepository.save(product);
            
            System.out.println("📦 Reserved " + requestedQuantity + " units of " + 
                             product.getName() + " (New stock: " + newStock + ")");
        }
        
        System.out.println("🎉 Inventory reservation completed successfully!");
        return true;
    }

    /**
     * Release reserved inventory (for cancelled orders)
     */
    @Transactional
    public void releaseInventory(Map<Long, Integer> productQuantities) {
        System.out.println("🔄 Releasing inventory for cancelled order...");
        
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
            
            int newStock = product.getStockQuantity() + quantity;
            product.setStockQuantity(newStock);
            productRepository.save(product);
            
            System.out.println("📦 Released " + quantity + " units of " + 
                             product.getName() + " (New stock: " + newStock + ")");
        }
        
        System.out.println("✅ Inventory release completed!");
    }

    /**
     * Check if products are available without reserving
     */
    public boolean checkAvailability(Map<Long, Integer> productQuantities) {
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer requestedQuantity = entry.getValue();
            
            Product product = productRepository.findById(productId)
                    .orElse(null);
            
            if (product == null || !product.getActive() || 
                product.getStockQuantity() < requestedQuantity) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get current stock levels for multiple products
     */
    public Map<Long, Integer> getStockLevels(List<Long> productIds) {
        Map<Long, Integer> stockLevels = new HashMap<>();
        
        for (Long productId : productIds) {
            Product product = productRepository.findById(productId).orElse(null);
            if (product != null) {
                stockLevels.put(productId, product.getStockQuantity());
            } else {
                stockLevels.put(productId, 0);
            }
        }
        
        return stockLevels;
    }

    /**
     * Check if a single product has sufficient stock
     */
    public boolean hasStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId).orElse(null);
        return product != null && product.getActive() && product.getStockQuantity() >= quantity;
    }
}