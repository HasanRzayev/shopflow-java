package com.shopflow.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent implements Serializable {
    private Long orderId;
    private String orderNumber;
    private String reason;
    private Map<Long, Integer> productQuantities;
    private LocalDateTime timestamp;
}
