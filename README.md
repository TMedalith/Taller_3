# Orquestación de Entregas Express

Sistema de microservicios para gestionar entregas express implementando Event-Driven Architecture (EDA) con Kafka, Redis y MongoDB.

## Arquitectura

### Microservicios

1. **shipping-ops-producer** (Puerto 8087)
   - Recibe pedidos de envío vía REST API
   - Publica eventos Avro a Kafka
   - Guarda snapshots en Redis con TTL

2. **dispatch-orchestrator-consumer** (Puerto 8088)
   - Consume eventos de Kafka automáticamente
   - Implementa reglas de idempotencia y reintentos
   - Persiste en MongoDB con estados QUEUED/QUEUED_CACHE
   - Expone API de consulta

### Infraestructura

- **Kafka**: Message broker con Schema Registry
- **Redis**: Cache para snapshots (TTL 4 horas)
- **MongoDB**: Persistencia de eventos procesados
- **Zookeeper**: Coordinación de Kafka

## Inicio Rápido

### Prerrequisitos
- Docker y Docker Compose
- 8GB RAM disponible
- Puertos libres: 8087, 8088, 2181, 9092, 29092, 8081, 6379, 27018

### Levantar el Sistema

```bash
# Clonar y construir
docker compose up --build -d

# Verificar estado de servicios
docker compose ps

# Ver logs
docker compose logs -f shipping-ops-producer
docker compose logs -f dispatch-orchestrator-consumer
```

### Verificar Health

```bash
# Producer
curl -i http://localhost:8087/actuator/health

# Consumer
curl -i http://localhost:8088/api/health

# Kafka Topics
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list
```

## Casos de Prueba

### 1. Intento 1 (Primer envío)

```bash
curl -s -X POST http://localhost:8087/api/shipments \
  -H "Content-Type: application/json" \
  -d '{
    "shipmentId": "SHP-9001",
    "orderId": "ORD-12345",
    "customerId": "CUST-777",
    "address": "Calle Falsa 123",
    "city": "Lima",
    "postalCode": "15000",
    "serviceLevel": "EXPRESS",
    "requestedAt": "2025-11-10T10:30:00Z",
    "attemptNumber": 1,
    "correlationId": "corr-XYZ-123",
    "status": "NEW"
  }'
```

**Resultado esperado:**
- Producer: 202 Accepted con eventId
- Kafka: Mensaje en `logistics.shipments.v1`
- Redis: Snapshot en `ship:event:SHP-9001`
- Consumer: Insert en MongoDB con status=QUEUED

### 2. Consultar Evento

```bash
curl -s http://localhost:8088/api/shipments/SHP-9001 | jq .
```

**Resultado esperado:**
```json
{
  "id": "SHP-9001",
  "shipmentId": "SHP-9001",
  "orderId": "ORD-12345",
  "status": "QUEUED",
  "receivedAt": 1752143420123,
  ...
}
```

### 3. Intento 2 (Reintento con cache)

```bash
curl -s -X POST http://localhost:8087/api/shipments \
  -H "Content-Type: application/json" \
  -d '{
    "shipmentId": "SHP-9001",
    "orderId": "ORD-99999",
    "customerId": "CUST-888",
    "address": "Nueva Dirección 456",
    "city": "Arequipa",
    "postalCode": "04000",
    "serviceLevel": "STANDARD",
    "requestedAt": "2025-11-10T10:35:00Z",
    "attemptNumber": 2,
    "correlationId": "corr-XYZ-123",
    "status": "RETRY"
  }'
```

**Resultado esperado:**
- Consumer: Lee snapshot de Redis
- Merge: Campos del snapshot tienen prioridad
- MongoDB: Upsert con status=QUEUED_CACHE y processedAt

### 4. Duplicado (Intento 1 repetido)

```bash
# Repetir el primer request exactamente igual
curl -s -X POST http://localhost:8087/api/shipments \
  -H "Content-Type: application/json" \
  -d '{
    "shipmentId": "SHP-9001",
    "orderId": "ORD-12345",
    ...
    "attemptNumber": 1,
    ...
  }'
```

**Resultado esperado:**
- Consumer: ACK duplicado, log de advertencia
- MongoDB: Sin cambios (idempotencia)

### 5. Error Controlado (Sin shipmentId)

```bash
curl -s -X POST http://localhost:8087/api/shipments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-ERROR",
    "customerId": "CUST-999",
    "attemptNumber": 1,
    ...
  }'
```

**Resultado esperado:**
- Producer: 400 Bad Request (validación)
- Si llega al consumer: Envío al DLT

## Endpoints

### Producer (8087)
- `POST /api/shipments` - Crear envío
- `GET /actuator/health` - Health check
- `GET /actuator/prometheus` - Métricas

### Consumer (8088)
- `GET /api/shipments/{shipmentId}` - Consultar envío
- `GET /api/health` - Health check
- `GET /actuator/prometheus` - Métricas

## Monitoreo

### Verificar Kafka Topics

```bash
# Listar topics
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list

# Ver mensajes en topic principal
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic logistics.shipments.v1 \
  --from-beginning

# Ver mensajes en DLT
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic logistics.shipments.v1.DLT \
  --from-beginning
```

### Verificar Redis

```bash
# Conectar a Redis
docker exec -it redis redis-cli

# Ver snapshots
redis-cli> KEYS ship:event:*
redis-cli> GET ship:event:SHP-9001
redis-cli> TTL ship:event:SHP-9001
```

### Verificar MongoDB

```bash
# Conectar a MongoDB
docker exec -it mongo mongosh -u root -p password

# Consultar colección
use shipmentsdb
db.shipments_queue.find().pretty()
db.shipments_queue.findOne({shipmentId: "SHP-9001"})
```

## Reglas de Negocio Implementadas

### Primer Intento (attemptNumber=1)
- Si no existe shipmentId en MongoDB → INSERT con status="QUEUED"
- Si existe → ACK duplicado + log advertencia

### Segundo Intento (attemptNumber=2)
- Lee snapshot de Redis `ship:event:{shipmentId}`
- Merge: campos del snapshot tienen prioridad
- UPSERT en MongoDB con status="QUEUED_CACHE" + processedAt

### Manejo de Errores
- Evento sin shipmentId → DLT + ACK
- Error de procesamiento → DLT + ACK
- Redis no disponible → Continúa sin snapshot

## Estructura del Proyecto

```
├── shipping-ops-producer/
│   ├── src/main/java/com/logistics/shipping_ops_producer/
│   │   ├── api/
│   │   ├── config/
│   │   ├── domain/
│   │   └── infrastructure/
│   ├── src/main/avro/ShipmentEvent.avsc
│   └── Dockerfile
├── dispatch-orchestrator-consumer/
│   ├── src/main/java/com/logistics/dispatch_orchestrator_consumer/
│   │   ├── api/
│   │   ├── config/
│   │   ├── domain/
│   │   └── infrastructure/
│   └── Dockerfile
├── docker-compose.yml
└── README.md
```

## Troubleshooting

### Services no inician
```bash
# Verificar logs de dependencias
docker compose logs zookeeper kafka schema-registry

# Reiniciar servicios problemáticos
docker compose restart shipping-ops-producer
```

### Consumer no procesa mensajes
```bash
# Ver logs del consumer
docker compose logs -f dispatch-orchestrator-consumer

# Verificar consumer group
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group dispatch-orchestrator-consumer
```

### Schema Registry issues
```bash
# Verificar schemas registrados
curl -s http://localhost:8081/subjects | jq .

# Ver schema específico
curl -s http://localhost:8081/subjects/logistics.shipments.v1-value/versions/latest | jq .
```
