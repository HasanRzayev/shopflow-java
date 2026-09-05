package com.shopflow.inventory;

import com.shopflow.common.exception.InsufficientStockException;
import com.shopflow.product.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        Product product = Product.builder().id(1L).sku("TEST-SKU").build();
        inventory = Inventory.builder()
                .id(1L)
                .product(product)
                .quantity(10)
                .reservedQuantity(2)
                .version(1L)
                .build();
    }

    @Test
    void reserveStock_Success() {
        when(inventoryRepository.findByProductIdWithOptimisticLock(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

        inventoryService.reserveStock(1L, 3);

        assertEquals(5, inventory.getReservedQuantity());
        assertEquals(5, inventory.getAvailableQuantity());
        verify(inventoryRepository, times(1)).save(inventory);
    }

    @Test
    void reserveStock_InsufficientStock_ThrowsException() {
        when(inventoryRepository.findByProductIdWithOptimisticLock(1L)).thenReturn(Optional.of(inventory));

        assertThrows(InsufficientStockException.class, () ->
                inventoryService.reserveStock(1L, 20));

        verify(inventoryRepository, never()).save(any());
    }
}
