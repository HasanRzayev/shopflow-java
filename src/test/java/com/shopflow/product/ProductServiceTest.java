package com.shopflow.product;

import com.shopflow.inventory.Inventory;
import com.shopflow.inventory.InventoryRepository;
import com.shopflow.inventory.InventoryService;
import com.shopflow.product.dto.CreateProductRequest;
import com.shopflow.product.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProduct_Success() {
        CreateProductRequest request = CreateProductRequest.builder()
                .sku("LAPTOP-MAC-M3")
                .name("MacBook Pro M3")
                .description("Apple Silicon laptop")
                .price(new BigDecimal("2499.00"))
                .initialStock(15)
                .build();

        when(productRepository.existsBySku("LAPTOP-MAC-M3")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(i -> {
            Product p = i.getArgument(0);
            p.setId(50L);
            return p;
        });
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals("LAPTOP-MAC-M3", response.getSku());
        assertEquals(15, response.getAvailableStock());

        verify(productRepository, times(1)).save(any(Product.class));
        verify(inventoryRepository, times(1)).save(any(Inventory.class));
    }
}
