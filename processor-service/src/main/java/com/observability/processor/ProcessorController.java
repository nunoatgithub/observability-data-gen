package com.observability.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProcessorController {

    private static final Logger logger = LoggerFactory.getLogger(ProcessorController.class);
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ProcessorController(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping("/process")
    public Map<String, Object> process() {
        logger.info("Processor service received request");
        
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("service", "processor-service");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("processedData", "Processed by Processor Service");
        response.put("data", "Final data from Processor");

        logger.info("Processor service returning processed data");
        return response;
    }

    @KafkaListener(topics = "gateway-processor-request")
    public void handleGatewayRequest(@Payload String message,
                                   @Header(KafkaHeaders.RECEIVED_KEY) String correlationId) {
        logger.info("Processor service received Kafka request from gateway with correlationId: {} and message: {}", correlationId, message);

        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String response = String.format(
            "{\"service\":\"processor-service\",\"timestamp\":\"%s\",\"processedData\":\"Processed by Processor Service via Kafka\",\"data\":\"Final data from Processor\"}",
            LocalDateTime.now().toString()
        );

        kafkaTemplate.send("gateway-processor-reply", correlationId, response);
        logger.info("Processor service sent Kafka reply to gateway with correlationId: {}", correlationId);
    }

    @KafkaListener(topics = "gateway-processor-fire-forget")
    public void handleGatewayFireForgetRequest(@Payload String message,
                                             @Header(KafkaHeaders.RECEIVED_KEY) String correlationId) {
        logger.info("Processor service received fire-and-forget request from gateway with correlationId: {} and message: {}", correlationId, message);

        try {
            Thread.sleep(25);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info("Processor service processed fire-and-forget request, no response sent");
    }
}
