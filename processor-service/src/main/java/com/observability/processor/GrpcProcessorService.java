package com.observability.processor;

import com.observability.processor.grpc.ProcessRequest;
import com.observability.processor.grpc.ProcessResponse;
import com.observability.processor.grpc.ProcessorServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@GrpcService
public class GrpcProcessorService extends ProcessorServiceGrpc.ProcessorServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(GrpcProcessorService.class);

    @Override
    public void processData(ProcessRequest request, StreamObserver<ProcessResponse> responseObserver) {
        logger.info("Processor service received gRPC request with message: {}", request.getMessage());

        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ProcessResponse response = ProcessResponse.newBuilder()
                .setService("processor-service")
                .setTimestamp(LocalDateTime.now().toString())
                .setProcessedData("Processed by Processor Service via gRPC")
                .setData("Final data from Processor via gRPC")
                .build();

        logger.info("Processor service sending gRPC response");

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}

