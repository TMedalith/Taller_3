package com.logistics.dispatch_orchestrator_consumer.infrastructure.kafka;

import reactor.kafka.receiver.ReceiverOffset;

public record EventMessage<T>(T payload, ReceiverOffset offset, Object rawValue) {
}