package com.shopflow.inventory;

import com.shopflow.common.exception.InsufficientStockException;
import com.shopflow.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public int getAvailableStock(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .map(Inventory::getAvailableQuantity)
                .orElse(0);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void reserveStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdWithOptimisticLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found for product: " + productId));

        if (inventory.getAvailableQuantity() < quantity) {
            throw new InsufficientStockException(String.format(
                    "Insufficient stock for product ID %d. Requested: %d, Available: %d",
                    productId, quantity, inventory.getAvailableQuantity()));
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventoryRepository.save(inventory);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void releaseStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdWithOptimisticLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found for product: " + productId));

        inventory.setReservedQuantity(Math.max(0, inventory.getReservedQuantity() - quantity));
        inventoryRepository.save(inventory);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void commitStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdWithOptimisticLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found for product: " + productId));

        inventory.setQuantity(Math.max(0, inventory.getQuantity() - quantity));
        inventory.setReservedQuantity(Math.max(0, inventory.getReservedQuantity() - quantity));
        inventoryRepository.save(inventory);
    }
}
