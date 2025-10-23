package com.observability.gateway;

import com.observability.processor.grpc.ProcessRequest;
import com.observability.processor.grpc.ProcessResponse;
import com.observability.processor.grpc.ProcessorServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

@RestController
public class GatewayController {

    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);
    private static final String DB_TEST_KEY = "test-key";
    private static final String ES_TEST_DOC_ID = "test-doc";

    @GrpcClient("processor-service")
    private ProcessorServiceGrpc.ProcessorServiceBlockingStub processorServiceStub;

    private final RestTemplate restTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KeyValueRepository keyValueRepository;
    private final SearchDocumentRepository searchDocumentRepository;
    private final ConcurrentHashMap<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();

    public GatewayController(RestTemplate restTemplate, KafkaTemplate<String, String> kafkaTemplate,
                           KeyValueRepository keyValueRepository, SearchDocumentRepository searchDocumentRepository) {
        this.restTemplate = restTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.keyValueRepository = keyValueRepository;
        this.searchDocumentRepository = searchDocumentRepository;
    }

    @KafkaListener(topics = "gateway-processor-reply")
    public void handleProcessorReply(@Payload String message,
                                   @Header(KafkaHeaders.RECEIVED_KEY) String correlationId) {
        logger.info("Gateway received Kafka reply with correlationId: {}", correlationId);

        CompletableFuture<String> future = pendingRequests.remove(correlationId);
        if (future != null) {
            future.complete(message);
        }
    }

    @GetMapping("/rest-chain")
    public Map<String, Object> restChain() {
        logger.info("Gateway service received rest-chain request");

        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info("Calling processor service");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> processorData = restTemplate.getForObject(
                "http://processor-service:9981/api/process",
                Map.class
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Hello World!");
            response.put("service", "gateway-service");
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("processorResponse", processorData);

            logger.info("Gateway service returning rest-chain response");
            return response;
        } catch (Exception e) {
            logger.error("Error calling processor service", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Hello World - Error!");
            errorResponse.put("service", "gateway-service");
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            errorResponse.put("error", "Failed to call processor service");
            return errorResponse;
        }
    }

    @GetMapping("/kafka-rr")
    public Map<String, Object> kafkaRequestReply() {
        logger.info("Gateway service received kafka-rr request");

        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info("Sending Kafka request to processor service");
        
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(correlationId, future);
        
        try {
            kafkaTemplate.send("gateway-processor-request", correlationId, "kafka-rr-request");
            String processorResponse = future.get(10, TimeUnit.SECONDS);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Hello World via Kafka!");
            response.put("service", "gateway-service");
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("processorResponse", processorResponse);

            logger.info("Gateway service returning kafka-rr response");
            return response;
        } catch (Exception e) {
            logger.error("Error in kafka-rr Kafka request-reply", e);
            pendingRequests.remove(correlationId);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Hello World via Kafka - Error!");
            errorResponse.put("service", "gateway-service");
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            errorResponse.put("error", "Failed to get response from processor: " + e.getMessage());
            return errorResponse;
        }
    }

    @GetMapping("/kafka-ff")
    public Map<String, Object> kafkaFireAndForget() {
        logger.info("Gateway service received kafka-ff request");

        try {
            Thread.sleep(15);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info("Sending fire-and-forget Kafka request to processor service");

        String correlationId = UUID.randomUUID().toString();
        kafkaTemplate.send("gateway-processor-fire-forget", correlationId, "kafka-ff-request");

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello World Fire-and-Forget!");
        response.put("service", "gateway-service");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("correlationId", correlationId);
        response.put("status", "Request sent, no response expected");

        logger.info("Gateway service returning kafka-ff response immediately");
        return response;
    }

    @GetMapping("/db-ops")
    public Map<String, Object> databaseOperations() {
        logger.info("Gateway service received db-ops request - testing database operations");

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        LocalDateTime now = LocalDateTime.now();
        String newValue = "Updated at " + now.toString();

        // Write to database
        logger.info("Writing to database with key: {}", DB_TEST_KEY);
        KeyValueEntity entity = new KeyValueEntity(DB_TEST_KEY, newValue, now);
        keyValueRepository.save(entity);
        logger.info("Successfully wrote to database");

        // Read from database
        logger.info("Reading from database with key: {}", DB_TEST_KEY);
        Optional<KeyValueEntity> readEntity = keyValueRepository.findById(DB_TEST_KEY);
        logger.info("Successfully read from database");

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello World with Database!");
        response.put("service", "gateway-service");
        response.put("timestamp", now.toString());
        response.put("databaseOperation", "write and read");
        response.put("key", DB_TEST_KEY);

        if (readEntity.isPresent()) {
            KeyValueEntity dbEntity = readEntity.get();
            Map<String, String> dbData = new HashMap<>();
            dbData.put("key", dbEntity.getKey());
            dbData.put("value", dbEntity.getValue());
            dbData.put("updatedAt", dbEntity.getUpdatedAt().toString());
            response.put("databaseRecord", dbData);
        } else {
            response.put("databaseRecord", "Not found");
        }

        logger.info("Gateway service returning db-ops response");
        return response;
    }

    @GetMapping("/es-ops")
    public Map<String, Object> elasticsearchOperations() {
        logger.info("Gateway service received es-ops request - testing Elasticsearch operations");

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        LocalDateTime now = LocalDateTime.now();
        String newContent = "Indexed at " + now;

        // Write to Elasticsearch
        logger.info("Writing to Elasticsearch with document ID: {}", ES_TEST_DOC_ID);
        SearchDocument document = new SearchDocument(ES_TEST_DOC_ID, newContent, now);
        searchDocumentRepository.save(document);
        logger.info("Successfully wrote to Elasticsearch");

        // Read from Elasticsearch
        logger.info("Reading from Elasticsearch with document ID: {}", ES_TEST_DOC_ID);
        Optional<SearchDocument> readDocument = searchDocumentRepository.findById(ES_TEST_DOC_ID);
        logger.info("Successfully read from Elasticsearch");

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello World with Elasticsearch!");
        response.put("service", "gateway-service");
        response.put("timestamp", now.toString());
        response.put("elasticsearchOperation", "write and read");
        response.put("documentId", ES_TEST_DOC_ID);

        if (readDocument.isPresent()) {
            SearchDocument esDoc = readDocument.get();
            Map<String, String> esData = new HashMap<>();
            esData.put("id", esDoc.getId());
            esData.put("content", esDoc.getContent());
            esData.put("timestamp", esDoc.getTimestamp().toString());
            response.put("elasticsearchDocument", esData);
        } else {
            response.put("elasticsearchDocument", "Not found");
        }

        logger.info("Gateway service returning es-ops response");
        return response;
    }

    @GetMapping("/grpc")
    public Map<String, Object> grpcCommunication() {
        logger.info("Gateway service received grpc request - testing gRPC communication");

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info("Sending gRPC request to processor service");

        try {
            ProcessRequest grpcRequest = ProcessRequest.newBuilder()
                    .setMessage("grpc-request")
                    .build();

            ProcessResponse grpcResponse = processorServiceStub.processData(grpcRequest);
            logger.info("Received gRPC response from processor service");

            Map<String, Object> processorData = new HashMap<>();
            processorData.put("service", grpcResponse.getService());
            processorData.put("timestamp", grpcResponse.getTimestamp());
            processorData.put("processedData", grpcResponse.getProcessedData());
            processorData.put("data", grpcResponse.getData());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Hello World with gRPC!");
            response.put("service", "gateway-service");
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("grpcCommunication", "synchronous");
            response.put("processorResponse", processorData);

            logger.info("Gateway service returning grpc response");
            return response;
        } catch (Exception e) {
            logger.error("Error in grpc gRPC request", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Hello World with gRPC - Error!");
            errorResponse.put("service", "gateway-service");
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            errorResponse.put("error", "Failed to call processor service via gRPC: " + e.getMessage());
            return errorResponse;
        }
    }
}
