package com.shopflow.order;

import com.shopflow.auth.security.UserPrincipal;
import com.shopflow.common.dto.ApiResponse;
import com.shopflow.order.dto.CreateOrderRequest;
import com.shopflow.order.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order placement, payment confirmation, cancellation, and lifecycle status tracking")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place order and reserve inventory stock")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(currentUser.getId(), request);
        return new ResponseEntity<>(ApiResponse.ok("Order placed and stock reserved", response), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/my-orders")
    @Operation(summary = "Get paginated orders of authenticated customer")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<OrderResponse> response = orderService.getUserOrders(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/confirm-payment")
    @Operation(summary = "Confirm payment and commit reserved stock")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmPayment(
            @PathVariable Long id,
            @RequestParam String paymentReference) {
        OrderResponse response = orderService.confirmPayment(id, paymentReference);
        return ResponseEntity.ok(ApiResponse.ok("Payment confirmed and stock committed", response));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel order and release reserved stock")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long id,
            @RequestParam(defaultValue = "Cancelled by user") String reason) {
        OrderResponse response = orderService.cancelOrder(id, reason);
        return ResponseEntity.ok(ApiResponse.ok("Order cancelled and stock released", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Advance order lifecycle state (Staff only)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        OrderResponse response = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Order status transitioned", response));
    }
}
