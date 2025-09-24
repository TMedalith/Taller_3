package com.logistics.shipping_ops_producer.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.Instant;

@Data
public class ShipmentRequest {
    @NotBlank(message = "shipmentId is required")
    private String shipmentId;

    @NotBlank(message = "orderId is required")
    private String orderId;

    @NotBlank(message = "customerId is required")
    private String customerId;

    @NotBlank(message = "address is required")
    private String address;

    @NotBlank(message = "city is required")
    private String city;

    @NotBlank(message = "postalCode is required")
    private String postalCode;

    @NotBlank(message = "serviceLevel is required")
    private String serviceLevel;

    @NotNull(message = "requestedAt is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant requestedAt;

    @NotNull(message = "attemptNumber is required")
    @Positive(message = "attemptNumber must be positive")
    private Integer attemptNumber;

    @NotBlank(message = "correlationId is required")
    private String correlationId;

    private String status = "NEW";
}