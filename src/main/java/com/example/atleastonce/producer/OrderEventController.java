package com.example.atleastonce.producer;

import com.example.atleastonce.model.OrderEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderEventController {

    private final OrderEventProducer producer;

    public OrderEventController(OrderEventProducer producer) {
        this.producer = producer;
    }

    @PostMapping
    public ResponseEntity<String> publish(@RequestBody OrderEvent event) {
        producer.publish(event);
        return ResponseEntity.accepted().body("Event queued: " + event.orderId());
    }
}
