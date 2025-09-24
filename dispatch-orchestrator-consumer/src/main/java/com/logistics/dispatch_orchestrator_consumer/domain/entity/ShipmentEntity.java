package com.logistics.dispatch_orchestrator_consumer.domain.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "shipments_queue")
public class ShipmentEntity {

    @Id
    private String id; // shipmentId as MongoDB _id

    private String shipmentId;
    private String orderId;
    private String customerId;
    private String address;
    private String city;
    private String postalCode;
    private String serviceLevel;
    private Long requestedAt;
    private String status; // QUEUED, QUEUED_CACHE
    private Long receivedAt;
    private Long processedAt;
    private String correlationId;
    private String rawPayload; // For audit purposes
}