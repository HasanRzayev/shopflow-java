package com.shopflow.order;

import com.shopflow.common.exception.InvalidOrderStateException;
import com.shopflow.event.OrderEventPublisher;
import com.shopflow.inventory.InventoryService;
import com.shopflow.order.dto.CreateOrderRequest;
import com.shopflow.order.dto.OrderItemRequest;
import com.shopflow.order.dto.OrderResponse;
import com.shopflow.product.Product;
import com.shopflow.product.ProductRepository;
import com.shopflow.user.Role;
import com.shopflow.user.User;
import com.shopflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InventoryService inventoryService;

    @Spy
    private OrderStateMachine orderStateMachine = new OrderStateMachine();

    @Mock
    private OrderEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private User customer;
    private Product product;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(1L)
                .email("buyer@shopflow.internal")
                .fullName("Alice Customer")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        product = Product.builder()
                .id(10L)
                .sku("IPHONE-15-PRO")
                .name("iPhone 15 Pro Max")
                .price(new BigDecimal("1999.00"))
                .active(true)
                .build();
    }

    @Test
    void createOrder_Success_ReservesStock() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("Baku, Nizami st. 45")
                .items(List.of(
                        OrderItemRequest.builder().productId(10L).quantity(2).build()
                ))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId(100L);
            return o;
        });

        OrderResponse response = orderService.createOrder(1L, request);

        assertNotNull(response);
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(new BigDecimal("3998.00"), response.getTotalAmount());
        assertEquals(1, response.getItems().size());

        verify(inventoryService, times(1)).reserveStock(10L, 2);
        verify(eventPublisher, times(1)).publishOrderCreated(any());
    }

    @Test
    void confirmPayment_Success_CommitsStock() {
        Order pendingOrder = Order.builder()
                .id(100L)
                .orderNumber("ORD-123456789012")
                .user(customer)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("3998.00"))
                .shippingAddress("Baku, Nizami st. 45")
                .items(new ArrayList<>())
                .build();

        OrderItem item = OrderItem.builder()
                .id(1L)
                .order(pendingOrder)
                .product(product)
                .quantity(2)
                .unitPrice(new BigDecimal("1999.00"))
                .subtotal(new BigDecimal("3998.00"))
                .build();
        pendingOrder.getItems().add(item);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse response = orderService.confirmPayment(100L, "PAY-999");

        assertNotNull(response);
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
        assertEquals("PAY-999", response.getPaymentReference());

        verify(inventoryService, times(1)).commitStock(10L, 2);
        verify(eventPublisher, times(1)).publishPaymentCompleted(any());
    }

    @Test
    void cancelOrder_PendingState_ReleasesStock() {
        Order pendingOrder = Order.builder()
                .id(100L)
                .orderNumber("ORD-123456789012")
                .user(customer)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("1999.00"))
                .shippingAddress("Baku, Nizami st. 45")
                .items(new ArrayList<>())
                .build();

        OrderItem item = OrderItem.builder()
                .id(1L)
                .order(pendingOrder)
                .product(product)
                .quantity(1)
                .unitPrice(new BigDecimal("1999.00"))
                .subtotal(new BigDecimal("1999.00"))
                .build();
        pendingOrder.getItems().add(item);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse response = orderService.cancelOrder(100L, "Changed my mind");

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, response.getStatus());

        verify(inventoryService, times(1)).releaseStock(10L, 1);
        verify(eventPublisher, times(1)).publishOrderCancelled(any());
    }

    @Test
    void updateOrderStatus_InvalidTransition_ThrowsException() {
        Order deliveredOrder = Order.builder()
                .id(200L)
                .orderNumber("ORD-999")
                .user(customer)
                .status(OrderStatus.DELIVERED)
                .totalAmount(BigDecimal.TEN)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findById(200L)).thenReturn(Optional.of(deliveredOrder));

        assertThrows(InvalidOrderStateException.class, () ->
                orderService.updateOrderStatus(200L, OrderStatus.PROCESSING));
    }
}
