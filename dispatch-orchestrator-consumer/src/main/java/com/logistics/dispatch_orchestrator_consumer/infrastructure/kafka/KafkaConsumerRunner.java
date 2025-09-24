package com.logistics.dispatch_orchestrator_consumer.infrastructure.kafka;

import com.logistics.dispatch_orchestrator_consumer.domain.service.ProcessingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumerRunner {

    private final KafkaRxConsumer consumer;
    private final ProcessingService processingService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @PostConstruct
    public void startConsumer() {
        log.info("Starting Kafka consumer for shipment events");

        consumer.stream()
                .doOnNext(message -> log.debug("Processing message: {}",
                        message.payload() != null ? message.payload().getShipmentId() : "null"))
                .flatMap(message -> {
                    if (message.payload() == null) {
                        log.warn("Null payload received, sending to DLT");
                        return sendToDlt(message)
                                .then(acknowledgeMessage(message));
                    }

                    return processingService.processShipmentEvent(message.payload(), message.rawValue())
                            .then(acknowledgeMessage(message))
                            .onErrorResume(error -> {
                                log.error("Error processing shipment event, sending to DLT: shipmentId={}",
                                        message.payload().getShipmentId(), error);
                                return sendToDlt(message)
                                        .then(acknowledgeMessage(message));
                            });
                })
                .onErrorContinue((error, obj) -> log.error("Error in consumer stream", error))
                .subscribe();

        log.info("Kafka consumer started successfully");
    }

    private reactor.core.publisher.Mono<Void> acknowledgeMessage(EventMessage<?> message) {
        return reactor.core.publisher.Mono.fromRunnable(() -> {
            message.offset().acknowledge();
            log.debug("Message acknowledged");
        });
    }

    private reactor.core.publisher.Mono<Void> sendToDlt(EventMessage<?> message) {
        return reactor.core.publisher.Mono.fromRunnable(() -> {
            try {
                String dltTopic = "logistics.shipments.v1.DLT";
                kafkaTemplate.send(dltTopic, message.rawValue());
                log.info("Message sent to DLT: topic={}", dltTopic);
            } catch (Exception e) {
                log.error("Failed to send message to DLT", e);
            }
        });
    }
}