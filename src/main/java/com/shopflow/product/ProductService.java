package com.shopflow.product;

import com.shopflow.common.exception.BusinessException;
import com.shopflow.common.exception.ResourceNotFoundException;
import com.shopflow.inventory.Inventory;
import com.shopflow.inventory.InventoryRepository;
import com.shopflow.inventory.InventoryService;
import com.shopflow.product.dto.CreateProductRequest;
import com.shopflow.product.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new BusinessException("Product with SKU already exists: " + request.getSku(), HttpStatus.CONFLICT);
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));
        }

        Product product = Product.builder()
                .sku(request.getSku().trim().toUpperCase())
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(category)
                .active(true)
                .build();

        Product savedProduct = productRepository.save(product);

        Inventory inventory = Inventory.builder()
                .product(savedProduct)
                .quantity(request.getInitialStock() != null ? request.getInitialStock() : 0)
                .reservedQuantity(0)
                .build();
        inventoryRepository.save(inventory);

        return ProductResponse.fromEntity(savedProduct, inventory.getAvailableQuantity());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        int availableStock = inventoryService.getAvailableStock(id);
        return ProductResponse.fromEntity(product, availableStock);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String search, String categorySlug, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Specification<Product> spec = ProductSpecification.filter(search, categorySlug, minPrice, maxPrice);
        Page<Product> page = productRepository.findAll(spec, pageable);
        return page.map(p -> ProductResponse.fromEntity(p, inventoryService.getAvailableStock(p.getId())));
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setActive(false);
        productRepository.save(product);
    }
}
