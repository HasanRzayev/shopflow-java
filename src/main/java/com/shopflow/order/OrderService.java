package com.shopflow.order;

import com.shopflow.common.exception.BusinessException;
import com.shopflow.common.exception.ResourceNotFoundException;
import com.shopflow.event.OrderCancelledEvent;
import com.shopflow.event.OrderCreatedEvent;
import com.shopflow.event.OrderEventPublisher;
import com.shopflow.event.PaymentCompletedEvent;
import com.shopflow.inventory.InventoryService;
import com.shopflow.order.dto.CreateOrderRequest;
import com.shopflow.order.dto.OrderItemRequest;
import com.shopflow.order.dto.OrderResponse;
import com.shopflow.product.Product;
import com.shopflow.product.ProductRepository;
import com.shopflow.user.User;
import com.shopflow.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final OrderStateMachine orderStateMachine;
    private final OrderEventPublisher eventPublisher;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String orderNumber = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .status(OrderStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal runningTotal = BigDecimal.ZERO;
        Map<Long, Integer> productQuantities = new HashMap<>();

        for (OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemReq.getProductId()));

            if (!product.isActive()) {
                throw new BusinessException("Product is inactive: " + product.getName());
            }

            inventoryService.reserveStock(product.getId(), itemReq.getQuantity());

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            runningTotal = runningTotal.add(subtotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getPrice())
                    .subtotal(subtotal)
                    .build();

            order.addItem(orderItem);
            productQuantities.put(product.getId(), itemReq.getQuantity());
        }

        order.setTotalAmount(runningTotal);
        Order saved = orderRepository.save(order);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(saved.getId())
                .orderNumber(saved.getOrderNumber())
                .userId(user.getId())
                .totalAmount(saved.getTotalAmount())
                .productQuantities(productQuantities)
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisher.publishOrderCreated(event);

        return OrderResponse.fromEntity(saved);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderResponse confirmPayment(Long orderId, String paymentReference) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        orderStateMachine.validateTransition(order.getStatus(), OrderStatus.CONFIRMED);

        for (OrderItem item : order.getItems()) {
            inventoryService.commitStock(item.getProduct().getId(), item.getQuantity());
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentReference(paymentReference);
        Order updated = orderRepository.save(order);

        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .orderId(updated.getId())
                .orderNumber(updated.getOrderNumber())
                .paymentReference(paymentReference)
                .amount(updated.getTotalAmount())
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisher.publishPaymentCompleted(event);

        return OrderResponse.fromEntity(updated);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderResponse cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        orderStateMachine.validateTransition(order.getStatus(), OrderStatus.CANCELLED);

        if (order.getStatus() == OrderStatus.PENDING) {
            for (OrderItem item : order.getItems()) {
                inventoryService.releaseStock(item.getProduct().getId(), item.getQuantity());
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);

        Map<Long, Integer> quantities = new HashMap<>();
        order.getItems().forEach(i -> quantities.put(i.getProduct().getId(), i.getQuantity()));

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(updated.getId())
                .orderNumber(updated.getOrderNumber())
                .reason(reason)
                .productQuantities(quantities)
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisher.publishOrderCancelled(event);

        return OrderResponse.fromEntity(updated);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus nextStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        orderStateMachine.validateTransition(order.getStatus(), nextStatus);
        order.setStatus(nextStatus);
        Order updated = orderRepository.save(order);

        return OrderResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return OrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findAllByUserId(userId, pageable)
                .map(OrderResponse::fromEntity);
    }
}
