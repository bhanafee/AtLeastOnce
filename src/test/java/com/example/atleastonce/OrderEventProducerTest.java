package com.example.atleastonce;

import com.example.atleastonce.model.OrderEvent;
import com.example.atleastonce.producer.OrderEventProducer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
        partitions = 1,
        topics = {OrderEventProducer.TOPIC},
        brokerProperties = {"listeners=PLAINTEXT://localhost:9093", "port=9093"}
)
class OrderEventProducerTest {

    @Autowired
    private OrderEventProducer producer;

    @Test
    void publish_sendsEventToTopic() throws Exception {
        var event = new OrderEvent("ord-1", "cust-1", "PLACED", 99.99);

        var future = producer.publish(event);

        var result = future.get(5, TimeUnit.SECONDS);
        assertThat(result.getRecordMetadata().topic()).isEqualTo(OrderEventProducer.TOPIC);
        assertThat(result.getRecordMetadata().offset()).isGreaterThanOrEqualTo(0);
    }
}
