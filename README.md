# Observability Data Generator

A multi-service Spring Boot system designed for testing OTEL-compliant distributed tracing, metrics, and logs. This project generates realistic telemetry data across multiple communication patterns including REST APIs, synchronous and asynchronous Kafka messaging, gRPC communication, database operations, and Elasticsearch search operations.

## Architecture

The system consists of two microservices that demonstrate different communication patterns:

1. **Gateway Service** (port 9980) - Public-facing service with multiple API endpoints, PostgreSQL database, and Elasticsearch integration
2. **Processor Service** (ports 9981, 9090) - Processing service that handles requests from the gateway via REST, Kafka, and gRPC

The services communicate through:
- **Synchronous REST calls** for traditional request-response patterns
- **Synchronous Kafka messaging** for request-reply patterns using Kafka as transport
- **Fire-and-Forget Kafka messaging** for non-blocking operations without waiting for responses
- **PostgreSQL database operations** for persistence and data access instrumentation
- **Elasticsearch operations** for search and document indexing instrumentation
- **gRPC calls** for high-performance binary protocol communication
- 
## Communication Patterns

### Pattern 1: Synchronous REST Call
**Endpoint:** `GET /rest-chain`
**Flow:** Gateway → Processor (REST call with response)

### Pattern 2: Synchronous Kafka Request-Reply  
**Endpoint:** `GET /kafka-rr`
**Flow:** Gateway → Processor (Kafka messaging with synchronous request-reply pattern)

### Pattern 3: Fire-and-Forget Kafka
**Endpoint:** `GET /kafka-ff`
**Flow:** Gateway → Processor (Kafka messaging without waiting for response)

### Pattern 4: Database Operations
**Endpoint:** `GET /db-ops`
**Flow:** Gateway → PostgreSQL (Database write followed by read operation)
**Purpose:** Tests database instrumentation, connection pooling, and query tracing

### Pattern 5: Elasticsearch Operations
**Endpoint:** `GET /es-ops`
**Flow:** Gateway → Elasticsearch (Document indexing followed by retrieval)
**Purpose:** Tests Elasticsearch instrumentation, document operations, and search query tracing

### Pattern 6: gRPC Communication
**Endpoint:** `GET /grpc`
**Flow:** Gateway → Processor (Synchronous gRPC call with binary protocol)
**Purpose:** Tests gRPC instrumentation, binary protocol tracing, and high-performance RPC communication

## OpenTelemetry Integration

The project is fully instrumented with OpenTelemetry for comprehensive observability:

### Automatic Instrumentation
- **Java Agent:** Uses OpenTelemetry Java agent for automatic instrumentation
- **Distributed Tracing:** Traces span across all services and communication methods
- **Database Tracing:** Automatic instrumentation of JDBC operations and SQL queries
- **gRPC Tracing:** Automatic instrumentation of gRPC client and server operations
- **Metrics Collection:** Automatic JVM, HTTP, Kafka, gRPC, and database metrics
- **Log Correlation:** Structured logging with trace correlation

### Telemetry Features
- **Cross-service trace propagation** via HTTP headers, Kafka message headers, and gRPC metadata
- **Database query tracing** with SQL statement capture and connection pool monitoring
- **gRPC request/response tracing** with method names, status codes, and timing
- **Automatic span creation** for HTTP requests, Kafka producers/consumers, gRPC calls, and database operations
- **Error tracking and exception spans**
- **Performance metrics** for all operations including database query times and gRPC call latencies
- **Service topology discovery** through distributed traces

## Technology Stack

### Core Technologies
- **Java 21** - Runtime environment
- **Spring Boot 3.5.6** - Application framework
- **Spring Data JPA** - Database access and ORM
- **PostgreSQL 15** - Relational database
- **Spring Data Elasticsearch** - Elasticsearch integration and document mapping
- **Elasticsearch 8.16.0** - Search and analytics engine
- **Spring Kafka** - Kafka integration with auto-configuration
- **Apache Kafka with KRaft** - Event streaming platform (no Zookeeper)
- **gRPC Java** - High-performance RPC framework
- **Protocol Buffers** - Binary serialization format for gRPC

### Observability Stack
- **OpenTelemetry Java Agent** - Automatic instrumentation
- **OpenTelemetry Spring Boot Starter** - Spring-native integration
- **OTLP Protocol** - Telemetry data transport
- **Structured JSON Logging** - Log correlation and analysis

### Infrastructure
- **Docker & Docker Compose** - Containerization and orchestration
- **PostgreSQL Container** - Database service with health checks
- **Elasticsearch Container** - Search engine with health checks
- **Kafka Topics** - Event-driven communication channels
- **gRPC Ports** - Binary protocol communication channels
- **Health Checks** - Service availability monitoring

## Prerequisites

- Java 21
- Maven 3.6+
- Docker
- Docker Compose

## Building

Build all services and create Docker images:

```bash
./build.sh
```

This will:
1. Build both Spring Boot applications with Maven
2. Create Docker images for each service
3. Tag them as `observability/[service-name]:1.0.0`
4. Include OpenTelemetry agent in each container

## Running

Start all services with Docker Compose:

```bash
docker-compose up
```

This starts:
- Gateway and Processor microservices with OpenTelemetry instrumentation
- PostgreSQL database with persistent volume
- Elasticsearch cluster (single-node) with persistent volume
- Kafka cluster with KRaft (no Zookeeper)
- Test OpenTelemetry collector for local telemetry capture
- Kafka topic initialization service

Stop all services:

```bash
docker-compose down
```

Remove all data including database volumes:

```bash
docker-compose down -v
```

## API Endpoints

### Gateway Service (Port 9980)

#### 1. Synchronous REST Pattern
```bash
curl http://localhost:9980/rest-chain
```
**Behavior:** Traditional REST call with synchronous communication and full response propagation.

#### 2. Synchronous Kafka Request-Reply Pattern  
```bash
curl http://localhost:9980/kafka-rr
```
**Behavior:** Uses Kafka for inter-service communication but maintains synchronous semantics with response correlation.

#### 3. Fire-and-Forget Kafka Pattern
```bash
curl http://localhost:9980/kafka-ff
```
**Behavior:** Asynchronous processing with no response waiting - demonstrates event-driven architecture.

#### 4. Database Operations Pattern
```bash
curl http://localhost:9980/db-ops
```
**Behavior:** Performs a database write followed by a read to the same key, demonstrating:
- JDBC connection instrumentation
- SQL query tracing
- Database connection pool monitoring
- Transaction management
- Data persistence operations

**Response includes:**
- Timestamp of operation
- Database key used (constant: "test-key")
- Full record retrieved including value and update timestamp

#### 5. Elasticsearch Operations Pattern
```bash
curl http://localhost:9980/es-ops
```
**Behavior:** Performs a document index operation followed by a retrieval to the same document ID, demonstrating:
- Elasticsearch client instrumentation
- Document indexing tracing
- Search and retrieval query tracing
- Index operations monitoring
- Document lifecycle operations

**Response includes:**
- Timestamp of operation
- Document ID used (constant: "test-doc")
- Full document retrieved including content and timestamp

#### 6. gRPC Communication Pattern
```bash
curl http://localhost:9980/grpc
```
**Behavior:** Performs a synchronous gRPC call to the processor service, demonstrating:
- gRPC client instrumentation
- Binary protocol communication
- gRPC method tracing
- High-performance RPC operations
- Protocol buffer serialization/deserialization
- gRPC status code handling

**Response includes:**
- Timestamp of operation
- Correlation ID for request tracking
- Processor response via gRPC (service name, timestamp, processed data)
- Communication type indicator (synchronous gRPC)

**gRPC Details:**
- **Protocol:** HTTP/2 with Protocol Buffers
- **Port:** 9090 (processor service)
- **Service:** ProcessorService
- **Method:** ProcessData
- **Negotiation:** PLAINTEXT (no TLS for testing)

## Kafka Topics

The system uses the following Kafka topics:

| Topic | Purpose | Pattern |
|-------|---------|---------|
| `gateway-processor-request` | Gateway → Processor requests | Request-Reply |
| `gateway-processor-reply` | Processor → Gateway responses | Request-Reply |
| `gateway-processor-fire-forget` | Gateway → Processor fire-and-forget | Fire-and-Forget |

## Configuration

### OpenTelemetry Configuration
- **Service Names:** Configured per service for proper identification
- **Collector Endpoints:** Dual endpoint configuration for production and testing
- **Sampling:** Configurable trace sampling rates
- **Resource Attributes:** Service metadata and version information

### Kafka Configuration
- **Endpoints:** `/rest-chain`, `/kafka-rr`, `/kafka-ff`, `/db-ops`, `/es-ops`, `/grpc`
- **Serialization:** String-based message serialization
- **Dependencies:** Processor Service (REST + Kafka + gRPC), PostgreSQL (for `/db-ops`), Elasticsearch (for `/es-ops`)
- **Topic Management:** Automatic topic creation and configuration

### Spring Boot Configuration
- **Profiles:** Environment-specific configurations
- **Health Checks:** Actuator endpoint configuration  
- **Logging:** Structured logging with trace correlation
- **Metrics:** Micrometer integration with OpenTelemetry

### Database Configuration
- **Connection Pool:** HikariCP (Spring Boot default)
- **JPA Open-in-View:** Disabled for better performance
- **SQL Logging:** Enabled for debugging (can be disabled in production)
- **Hibernate DDL:** Auto-update mode (use migrations in production)

## Services Detail

### Gateway Service
- **Port:** 9980 (HTTP)
- **Endpoints:** `/rest-chain`, `/kafka-rr`, `/kafka-ff`, `/db-ops`, `/es-ops`, `/grpc`
- **Role:** Public API gateway with multiple communication patterns
- **Dependencies:** Processor Service (REST + Kafka + gRPC), PostgreSQL (for `/db-ops`), Elasticsearch (for `/es-ops`)

### Processor Service
- **Port:** 9981 (HTTP), 9090 (gRPC)
- **Endpoints:** 
  - REST: `/api/process`
  - gRPC: `ProcessorService/ProcessData`
- **Role:** Middleware processing with multiple communication protocols
- **Dependencies:** None (leaf service)
- **Kafka:** Producer and Consumer for message processing
- **gRPC:** Server implementation for high-performance RPC

## Troubleshooting

### Common Issues
1. **Port Conflicts:** Ensure ports 9980-9981, 9090, 5318, 9092, 5432, 9200, 9300 are available
2. **Kafka Connectivity:** Check topic creation and broker connectivity
3. **Database Connectivity:** Verify PostgreSQL is running and accessible
4. **Elasticsearch Connectivity:** Verify Elasticsearch cluster health at http://localhost:9200
5. **gRPC Connectivity:** Verify processor service gRPC port 9090 is accessible
6. **OpenTelemetry Export:** Verify collector endpoints and network connectivity
7. **Java Version:** Ensure Java 21 is installed and configured
8. **Docker Resources:** Ensure adequate memory for all containers (recommend 8GB+ for Elasticsearch)

### Debugging Tools
- Check service logs: `docker-compose logs [service-name]`
- Monitor Kafka topics: Use Kafka console consumer tools
- Verify telemetry: Check local telemetry.log file
- Health checks: Access `/actuator/health` endpoints
- Trace debugging: Use OpenTelemetry logging exporter
- Test gRPC: Use `grpcurl` tool to test gRPC endpoints directly (see `processor-service/test_grpc.sh`)

### Testing gRPC Directly
The processor service includes a test script for direct gRPC testing:

```bash
# From the processor-service directory
cd processor-service
./test_grpc.sh
```

This requires `grpcurl` to be installed:
```bash
# Ubuntu/Debian
sudo apt-get install grpcurl

# macOS
brew install grpcurl
```
