package com.shopflow.worker;

import com.shopflow.event.OrderCancelledEvent;
import com.shopflow.event.OrderCreatedEvent;
import com.shopflow.event.PaymentCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InventoryWorker {

    @RabbitListener(queues = "${app.rabbitmq.queues.inventory}")
    public void handleOrderEvent(OrderCreatedEvent event) {
        log.info("[INVENTORY ASYNC WORKER] Processing reservation telemetry for Order: {} with {} items",
                event.getOrderNumber(), event.getProductQuantities().size());
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.payment}")
    public void handlePaymentEvent(PaymentCompletedEvent event) {
        log.info("[PAYMENT WORKER] Finalizing invoice ledger for Order: {} | PaymentRef: {} | Amount: {}",
                event.getOrderNumber(), event.getPaymentReference(), event.getAmount());
    }
}
