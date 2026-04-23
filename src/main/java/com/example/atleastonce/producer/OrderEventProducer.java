package com.example.atleastonce.producer;

import com.example.atleastonce.model.OrderEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);
    static final String TOPIC = "order-events";

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes an order event.
     * The @Retry annotation retries on transient send failures.
     * The @CircuitBreaker opens if the broker is repeatedly unavailable.
     */
    @Retry(name = "orderProducer")
    @CircuitBreaker(name = "orderProducer", fallbackMethod = "publishFallback")
    public CompletableFuture<SendResult<String, OrderEvent>> publish(OrderEvent event) {
        log.info("Publishing event: orderId={}", event.orderId());
        return kafkaTemplate.send(TOPIC, event.orderId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event: orderId={}", event.orderId(), ex);
                    } else {
                        log.info("Published event: orderId={} partition={} offset={}",
                                event.orderId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    // Called by Resilience4j when the circuit is open
    public CompletableFuture<SendResult<String, OrderEvent>> publishFallback(OrderEvent event, Throwable t) {
        log.warn("Circuit open — dropping event to dead-letter store: orderId={} reason={}", event.orderId(), t.getMessage());
        // In production: persist to a local outbox or dead-letter DB table
        return CompletableFuture.failedFuture(t);
    }
}
