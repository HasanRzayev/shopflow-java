package com.shopflow.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchangeName, "order.created", event);
            log.info("Published order created event for order: {}", event.getOrderNumber());
        } catch (Exception ex) {
            log.error("Failed to publish order created event", ex);
        }
    }

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchangeName, "payment.completed", event);
            log.info("Published payment completed event for order: {}", event.getOrderNumber());
        } catch (Exception ex) {
            log.error("Failed to publish payment completed event", ex);
        }
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchangeName, "order.cancelled", event);
            log.info("Published order cancelled event for order: {}", event.getOrderNumber());
        } catch (Exception ex) {
            log.error("Failed to publish order cancelled event", ex);
        }
    }
}
