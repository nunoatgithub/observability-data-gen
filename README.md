# Observability Data Generator (Java)

A tiny two-service Spring Boot app to generate realistic telemetry (traces, metrics, logs) across multiple communication patterns. It’s meant for demos and testing—not as a production service.

## Quick start

1) Build images
```bash
./build.sh
```

2) Start stack
```bash
docker-compose up -d
```

3) Drive traffic (gRPC documented last, see Endpoints)
```bash
curl -s http://localhost:9980/rest-chain
curl -s http://localhost:9980/kafka-rr
curl -s http://localhost:9980/kafka-ff
curl -s http://localhost:9980/db-ops
curl -s http://localhost:9980/es-ops
curl -s http://localhost:9980/grpc
```

4) Verify telemetry locally
```bash
tail -f otel-logs/telemetry.log
```

5) Stop
```bash
docker-compose down
```

## Telemetry flow (OTLP over HTTP)

This app only produces telemetry; it doesn’t store or visualize it. A collector ingests the data.

- Protocol: OTLP over HTTP (http/protobuf), not OTLP/gRPC
- Default wiring (Compose):
  - Services export to `otel-test-collector:5318` (OTLP/HTTP)
  - The local test collector writes to `otel-logs/telemetry.log` and proxies to your host at `http://host.docker.internal:4318`
  - Change the proxy target in `otel-collector-config.yaml` at `exporters.otlphttp/production.endpoint`
- Ports you’ll see:
  - 5318: OTLP HTTP receiver on the local test collector
  - 4318: Typical OTLP HTTP endpoint on your external/vendor collector
  - 9090: gRPC between gateway and processor (demo endpoint only; not telemetry)

Point telemetry to your own collector/vendor
- Direct: set `OTEL_EXPORTER_OTLP_ENDPOINT=http://<collector-host>:4318` and keep `OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf`
- Via local proxy (default): keep services pointing at `otel-test-collector:5318` and change `otel-collector-config.yaml` exporter target

Quick checks
- Collector logs: `docker-compose logs otel-test-collector`
- Local spans: `tail -f otel-logs/telemetry.log`

## Endpoints (Gateway 9980)

1) REST request-response
- `GET /rest-chain` → Gateway → Processor (REST)

2) Kafka request-reply (sync semantics)
- `GET /kafka-rr` → Gateway ↔ Processor (Kafka RR)

3) Kafka fire-and-forget
- `GET /kafka-ff` → Gateway → Processor (Kafka FF)

4) Database operations
- `GET /db-ops` → Gateway → PostgreSQL (write + read)

5) Elasticsearch operations
- `GET /es-ops` → Gateway → Elasticsearch (index + get)

6) gRPC call (documented last)
- `GET /grpc` → Gateway → Processor (gRPC on 9090)
- Note: This gRPC is only for the demo pattern; telemetry export still uses OTLP/HTTP.

## Architecture & ports

- Services
  - Gateway service: 9980 (HTTP)
  - Processor service: 9981 (HTTP), 9090 (gRPC)
- Infra
  - Kafka: 9092 (host), 29092 (in-cluster)
  - PostgreSQL: 5432
  - Elasticsearch: 9200/9300
  - Local OTEL collector: 5318 (OTLP/HTTP)
  - External/vendor collector (typical): 4318 (OTLP/HTTP)

## Troubleshooting

- Telemetry not arriving at vendor/collector
  - Use HTTP 4318, not gRPC 4317; set `OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf`
  - If you see errors like “Connection refused … :443”, you likely pointed to an HTTPS-only URL without TLS or to the wrong port
- gRPC 9090 errors
  - That port is only for the demo gRPC endpoint; it’s not used for telemetry export
- Infra health
  - Check: `docker-compose ps` and service logs
  - Elasticsearch: http://localhost:9200
  - Kafka topics are created by the `kafka-topics-init` container
