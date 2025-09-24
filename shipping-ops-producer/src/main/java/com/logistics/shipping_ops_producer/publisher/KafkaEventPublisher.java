package com.logistics.shipping_ops_producer.publisher;

import com.logistics.events.ShipmentEvent;
import com.logistics.shipping_ops_producer.api.ShipmentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.shipments}")
    private String shipmentsTopic;

    public Mono<Boolean> publishShipmentEvent(String eventId, ShipmentRequest request) {
        return Mono.fromCallable(() -> {
            try {
                ShipmentEvent event = ShipmentEvent.newBuilder()
                        .setEventId(eventId)
                        .setShipmentId(request.getShipmentId())
                        .setOrderId(request.getOrderId())
                        .setCustomerId(request.getCustomerId())
                        .setAddress(request.getAddress())
                        .setCity(request.getCity())
                        .setPostalCode(request.getPostalCode())
                        .setServiceLevel(request.getServiceLevel())
                        .setRequestedAt(request.getRequestedAt())
                        .setAttemptNumber(request.getAttemptNumber())
                        .setCorrelationId(request.getCorrelationId())
                        .setStatus(request.getStatus())
                        .build();

                log.info("Publishing event to topic: {} with key: {}", shipmentsTopic, request.getShipmentId());

                kafkaTemplate.send(shipmentsTopic, request.getShipmentId(), event).get();

                log.info("Successfully published event with eventId: {} for shipmentId: {}",
                        eventId, request.getShipmentId());
                return true;
            } catch (Exception e) {
                log.error("Failed to publish event for shipmentId: {}", request.getShipmentId(), e);
                return false;
            }
        });
    }
}