package com.logistics.dispatch_orchestrator_consumer.domain.repository;

import com.logistics.dispatch_orchestrator_consumer.domain.entity.ShipmentEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ShipmentRepository extends ReactiveMongoRepository<ShipmentEntity, String> {

    Mono<ShipmentEntity> findByShipmentId(String shipmentId);

    Mono<Boolean> existsByShipmentId(String shipmentId);
}