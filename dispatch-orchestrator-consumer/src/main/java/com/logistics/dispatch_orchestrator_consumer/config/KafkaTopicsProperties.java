package com.logistics.dispatch_orchestrator_consumer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.topics")
public class KafkaTopicsProperties {
    private String main;
    private String dlt;
}