package com.logistics.dispatch_orchestrator_consumer.infrastructure.redis;

import com.logistics.dispatch_orchestrator_consumer.domain.repository.SnapshotCacheRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@Slf4j
public class SnapshotCacheRepositoryImpl implements SnapshotCacheRepository {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public SnapshotCacheRepositoryImpl(@Qualifier("reactiveStringRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<String> getSnapshot(String shipmentId) {
        String key = "ship:event:" + shipmentId;

        return redisTemplate.opsForValue()
                .get(key)
                .doOnSuccess(result -> {
                    if (result != null) {
                        log.info("Snapshot found in Redis for key: {}", key);
                    } else {
                        log.warn("No snapshot found in Redis for key: {}", key);
                    }
                })
                .doOnError(error -> log.error("Error retrieving snapshot from Redis for key: {}", key, error));
    }
}