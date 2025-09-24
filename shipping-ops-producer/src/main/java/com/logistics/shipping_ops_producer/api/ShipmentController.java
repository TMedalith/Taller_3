package com.logistics.shipping_ops_producer.api;

import com.logistics.shipping_ops_producer.publisher.KafkaEventPublisher;
import com.logistics.shipping_ops_producer.infrastructure.redis.RedisSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ShipmentController {

    private final KafkaEventPublisher kafkaEventPublisher;
    private final RedisSnapshotRepository redisSnapshotRepository;

    @PostMapping
    public Mono<ResponseEntity<ShipmentResponse>> createShipment(@Valid @RequestBody ShipmentRequest request) {
        log.info("Received shipment request for shipmentId: {}, attemptNumber: {}",
                 request.getShipmentId(), request.getAttemptNumber());

        String eventId = UUID.randomUUID().toString();

        return kafkaEventPublisher.publishShipmentEvent(eventId, request)
                .flatMap(success -> {
                    if (success) {
                        return redisSnapshotRepository.saveSnapshot(request.getShipmentId(), request)
                                .map(saved -> ResponseEntity.status(HttpStatus.ACCEPTED)
                                        .body(new ShipmentResponse(eventId, "Shipment event published successfully")))
                                .onErrorReturn(ResponseEntity.status(HttpStatus.ACCEPTED)
                                        .body(new ShipmentResponse(eventId, "Event published but snapshot save failed")));
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(new ShipmentResponse(eventId, "Failed to publish shipment event")));
                    }
                })
                .doOnSuccess(response -> log.info("Shipment processing completed for eventId: {}", eventId))
                .doOnError(error -> log.error("Error processing shipment request: ", error))
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ShipmentResponse(eventId, "Internal server error")));
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<String>> health() {
        return Mono.just(ResponseEntity.ok("OK"));
    }
}