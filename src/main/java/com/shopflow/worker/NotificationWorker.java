package com.shopflow.worker;

import com.shopflow.event.OrderCancelledEvent;
import com.shopflow.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationWorker {

    @RabbitListener(queues = "${app.rabbitmq.queues.notification}")
    public void handleOrderNotification(OrderCreatedEvent event) {
        log.info("[NOTIFICATION WORKER] Sending order confirmation email/SMS to User ID: {} for Order: {}",
                event.getUserId(), event.getOrderNumber());
    }
}
