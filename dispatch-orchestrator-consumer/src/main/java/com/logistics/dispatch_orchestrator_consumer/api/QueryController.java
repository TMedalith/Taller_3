package com.logistics.dispatch_orchestrator_consumer.api;

import com.logistics.dispatch_orchestrator_consumer.domain.entity.ShipmentEntity;
import com.logistics.dispatch_orchestrator_consumer.domain.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QueryController {

    private final ShipmentRepository repository;

    @GetMapping("/shipments/{shipmentId}")
    public Mono<ResponseEntity<ShipmentEntity>> getShipmentById(@PathVariable String shipmentId) {
        return repository.findByShipmentId(shipmentId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}