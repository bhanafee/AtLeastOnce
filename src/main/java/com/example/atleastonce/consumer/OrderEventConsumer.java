package com.example.atleastonce.consumer;

import com.example.atleastonce.model.OrderEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    /**
     * Consumes order events with manual acknowledgment.
     * The offset is committed only after process() succeeds, guaranteeing
     * at-least-once delivery. Resilience4j retries wrap the downstream call.
     */
    @KafkaListener(
            topics = "order-events",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, OrderEvent> record, Acknowledgment ack) {
        OrderEvent event = record.value();
        log.info("Received event: orderId={} partition={} offset={}",
                event.orderId(), record.partition(), record.offset());
        try {
            process(event);
            ack.acknowledge();  // commit offset only on success
        } catch (Exception ex) {
            log.error("Processing failed — will be retried by error handler: orderId={}", event.orderId(), ex);
            // Do NOT ack; DefaultErrorHandler in KafkaConfig will retry then route to DLT
            throw ex;
        }
    }

    @Retry(name = "orderConsumer")
    @CircuitBreaker(name = "orderConsumer")
    public void process(OrderEvent event) {
        // TODO: replace with real downstream call (DB write, HTTP call, etc.)
        log.info("Processing order: orderId={} status={} amount={}",
                event.orderId(), event.status(), event.amount());
    }
}
