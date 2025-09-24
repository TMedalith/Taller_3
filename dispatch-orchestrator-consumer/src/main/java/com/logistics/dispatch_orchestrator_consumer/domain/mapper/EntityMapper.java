package com.logistics.dispatch_orchestrator_consumer.domain.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.dispatch_orchestrator_consumer.domain.entity.ShipmentEntity;
import com.logistics.dispatch_orchestrator_consumer.domain.model.ShipmentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class EntityMapper {

    private final ObjectMapper objectMapper;

    public ShipmentEvent toEvent(GenericRecord avroRecord) {
        return ShipmentEvent.builder()
                .eventId(getString(avroRecord, "eventId"))
                .shipmentId(getString(avroRecord, "shipmentId"))
                .orderId(getString(avroRecord, "orderId"))
                .customerId(getString(avroRecord, "customerId"))
                .address(getString(avroRecord, "address"))
                .city(getString(avroRecord, "city"))
                .postalCode(getString(avroRecord, "postalCode"))
                .serviceLevel(getString(avroRecord, "serviceLevel"))
                .requestedAt(getLong(avroRecord, "requestedAt"))
                .attemptNumber(getInteger(avroRecord, "attemptNumber"))
                .correlationId(getString(avroRecord, "correlationId"))
                .status(getString(avroRecord, "status"))
                .build();
    }

    public ShipmentEntity toEntity(ShipmentEvent event, String status, String rawPayload) {
        long now = Instant.now().toEpochMilli();

        ShipmentEntity entity = new ShipmentEntity();
        entity.setId(event.getShipmentId());
        entity.setShipmentId(event.getShipmentId());
        entity.setOrderId(event.getOrderId());
        entity.setCustomerId(event.getCustomerId());
        entity.setAddress(event.getAddress());
        entity.setCity(event.getCity());
        entity.setPostalCode(event.getPostalCode());
        entity.setServiceLevel(event.getServiceLevel());
        entity.setRequestedAt(event.getRequestedAt());
        entity.setStatus(status);
        entity.setReceivedAt(now);
        entity.setCorrelationId(event.getCorrelationId());
        entity.setRawPayload(rawPayload);

        return entity;
    }

    public ShipmentEvent mergeWithSnapshot(ShipmentEvent event, String snapshotJson) {
        try {
            JsonNode snapshot = objectMapper.readTree(snapshotJson);

            return ShipmentEvent.builder()
                    .eventId(event.getEventId())
                    .shipmentId(getValueOrDefault(snapshot, "shipmentId", event.getShipmentId()))
                    .orderId(getValueOrDefault(snapshot, "orderId", event.getOrderId()))
                    .customerId(getValueOrDefault(snapshot, "customerId", event.getCustomerId()))
                    .address(getValueOrDefault(snapshot, "address", event.getAddress()))
                    .city(getValueOrDefault(snapshot, "city", event.getCity()))
                    .postalCode(getValueOrDefault(snapshot, "postalCode", event.getPostalCode()))
                    .serviceLevel(getValueOrDefault(snapshot, "serviceLevel", event.getServiceLevel()))
                    .requestedAt(event.getRequestedAt())
                    .attemptNumber(event.getAttemptNumber())
                    .correlationId(getValueOrDefault(snapshot, "correlationId", event.getCorrelationId()))
                    .status(event.getStatus())
                    .build();
        } catch (Exception e) {
            log.warn("Failed to merge with snapshot, using original event", e);
            return event;
        }
    }

    private String getString(GenericRecord record, String field) {
        Object value = record.get(field);
        return value != null ? value.toString() : null;
    }

    private Long getLong(GenericRecord record, String field) {
        Object value = record.get(field);
        return value != null ? (Long) value : null;
    }

    private Integer getInteger(GenericRecord record, String field) {
        Object value = record.get(field);
        return value != null ? (Integer) value : null;
    }

    private String getValueOrDefault(JsonNode snapshot, String field, String defaultValue) {
        JsonNode node = snapshot.get(field);
        return (node != null && !node.isNull()) ? node.asText() : defaultValue;
    }
}