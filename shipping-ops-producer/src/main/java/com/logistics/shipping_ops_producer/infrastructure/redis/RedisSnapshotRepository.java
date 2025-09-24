package com.logistics.shipping_ops_producer.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.shipping_ops_producer.api.ShipmentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Repository
@Slf4j
public class RedisSnapshotRepository {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${redis.snapshot.ttl:14400}")
    private long ttlSeconds;

    public RedisSnapshotRepository(@Qualifier("reactiveStringRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate,
                                   ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Mono<Boolean> saveSnapshot(String shipmentId, ShipmentRequest request) {
        String key = "ship:event:" + shipmentId;

        return Mono.fromCallable(() -> objectMapper.writeValueAsString(request))
                .flatMap(json -> {
                    log.info("Saving snapshot to Redis: key={}, ttl={}s", key, ttlSeconds);
                    return redisTemplate.opsForValue()
                            .set(key, json, Duration.ofSeconds(ttlSeconds))
                            .doOnSuccess(result -> log.info("Snapshot saved successfully: key={}", key))
                            .doOnError(error -> log.error("Failed to save snapshot: key={}", key, error))
                            .onErrorReturn(false);
                })
                .onErrorReturn(false);
    }
}