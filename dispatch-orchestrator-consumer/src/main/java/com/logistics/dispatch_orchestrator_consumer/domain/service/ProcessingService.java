package com.logistics.dispatch_orchestrator_consumer.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.dispatch_orchestrator_consumer.domain.entity.ShipmentEntity;
import com.logistics.dispatch_orchestrator_consumer.domain.mapper.EntityMapper;
import com.logistics.dispatch_orchestrator_consumer.domain.model.ShipmentEvent;
import com.logistics.dispatch_orchestrator_consumer.domain.repository.ShipmentRepository;
import com.logistics.dispatch_orchestrator_consumer.domain.repository.SnapshotCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessingService {

    private final ShipmentRepository shipmentRepository;
    private final SnapshotCacheRepository cacheRepository;
    private final EntityMapper entityMapper;
    private final ObjectMapper objectMapper;

    public Mono<Void> processShipmentEvent(ShipmentEvent event, Object rawValue) {
        if (event.getShipmentId() == null || event.getShipmentId().isEmpty()) {
            log.warn("Event without shipmentId, should be sent to DLT: eventId={}", event.getEventId());
            return Mono.error(new IllegalArgumentException("Event without shipmentId"));
        }

        String rawPayload = toRawPayload(rawValue);

        if (event.getAttemptNumber() == 1) {
            return processFirstAttempt(event, rawPayload);
        } else if (event.getAttemptNumber() == 2) {
            return processSecondAttempt(event, rawPayload);
        } else {
            log.warn("Unexpected attemptNumber: {}, treating as first attempt", event.getAttemptNumber());
            return processFirstAttempt(event, rawPayload);
        }
    }

    private Mono<Void> processFirstAttempt(ShipmentEvent event, String rawPayload) {
        log.info("Processing first attempt: shipmentId={}", event.getShipmentId());

        return shipmentRepository.existsByShipmentId(event.getShipmentId())
                .flatMap(exists -> {
                    if (exists) {
                        log.info("Duplicate event detected for first attempt: shipmentId={}", event.getShipmentId());
                        return Mono.empty(); // ACK duplicate
                    } else {
                        ShipmentEntity entity = entityMapper.toEntity(event, "QUEUED", rawPayload);
                        return shipmentRepository.save(entity)
                                .doOnSuccess(saved -> log.info("First attempt processed: shipmentId={}", saved.getShipmentId()))
                                .then();
                    }
                });
    }

    private Mono<Void> processSecondAttempt(ShipmentEvent event, String rawPayload) {
        log.info("Processing second attempt: shipmentId={}", event.getShipmentId());

        return cacheRepository.getSnapshot(event.getShipmentId())
                .map(snapshot -> entityMapper.mergeWithSnapshot(event, snapshot))
                .defaultIfEmpty(event)
                .flatMap(mergedEvent -> {
                    ShipmentEntity entity = entityMapper.toEntity(mergedEvent, "QUEUED_CACHE", rawPayload);
                    entity.setProcessedAt(Instant.now().toEpochMilli());

                    return shipmentRepository.save(entity)
                            .doOnSuccess(saved -> log.info("Second attempt processed with cache merge: shipmentId={}", saved.getShipmentId()))
                            .then();
                });
    }

    private String toRawPayload(Object rawValue) {
        try {
            return objectMapper.writeValueAsString(rawValue);
        } catch (Exception e) {
            log.warn("Failed to serialize raw payload", e);
            return rawValue != null ? rawValue.toString() : "{}";
        }
    }
}