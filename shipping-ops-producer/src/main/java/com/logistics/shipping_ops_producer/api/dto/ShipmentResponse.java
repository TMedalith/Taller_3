package com.logistics.shipping_ops_producer.api;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ShipmentResponse {
    private String eventId;
    private String message;
}