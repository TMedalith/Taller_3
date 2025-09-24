package com.logistics.dispatch_orchestrator_consumer.domain.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShipmentEvent {
    private String eventId;
    private String shipmentId;
    private String orderId;
    private String customerId;
    private String address;
    private String city;
    private String postalCode;
    private String serviceLevel;
    private Long requestedAt;
    private Integer attemptNumber;
    private String correlationId;
    private String status;
}