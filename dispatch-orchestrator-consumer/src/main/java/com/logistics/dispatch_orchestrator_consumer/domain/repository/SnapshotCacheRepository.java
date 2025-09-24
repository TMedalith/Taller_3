package com.logistics.dispatch_orchestrator_consumer.domain.repository;

import reactor.core.publisher.Mono;

public interface SnapshotCacheRepository {
    Mono<String> getSnapshot(String shipmentId);
}