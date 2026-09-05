package com.shopflow.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.queues.inventory}")
    private String inventoryQueueName;

    @Value("${app.rabbitmq.queues.notification}")
    private String notificationQueueName;

    @Value("${app.rabbitmq.queues.payment}")
    private String paymentQueueName;

    @Bean
    public TopicExchange shopflowExchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Queue inventoryQueue() {
        return QueueBuilder.durable(inventoryQueueName).build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(notificationQueueName).build();
    }

    @Bean
    public Queue paymentQueue() {
        return QueueBuilder.durable(paymentQueueName).build();
    }

    @Bean
    public Binding inventoryBinding(Queue inventoryQueue, TopicExchange shopflowExchange) {
        return BindingBuilder.bind(inventoryQueue).to(shopflowExchange).with("order.*");
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange shopflowExchange) {
        return BindingBuilder.bind(notificationQueue).to(shopflowExchange).with("order.*");
    }

    @Bean
    public Binding paymentBinding(Queue paymentQueue, TopicExchange shopflowExchange) {
        return BindingBuilder.bind(paymentQueue).to(shopflowExchange).with("payment.*");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
