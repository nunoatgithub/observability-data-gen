#!/bin/bash

# Direct gRPC test for Processor Service
# This script tests the processor service's gRPC endpoint directly

echo "=========================================="
echo "Testing Processor Service gRPC Endpoint"
echo "=========================================="
echo ""

# Check if grpcurl is installed
if ! command -v grpcurl &> /dev/null; then
    echo "❌ grpcurl is not installed. Install it with:"
    echo "   Ubuntu/Debian: sudo apt-get install grpcurl"
    echo "   macOS: brew install grpcurl"
    echo "   Or download from: https://github.com/fullstorydev/grpcurl/releases"
    exit 1
fi

echo "1. Checking if processor service is running..."
echo "--------------------------------------"

# Check if processor service is up
if ! curl -s http://localhost:9981/actuator/health > /dev/null 2>&1; then
    echo "❌ Processor service is not running on port 9981"
    echo "   Run: docker-compose up -d processor-service"
    exit 1
fi

echo "✓ Processor service is running (HTTP port 9981)"

# Check if gRPC port is accessible
if ! nc -z localhost 9090 2>/dev/null; then
    echo "❌ gRPC port 9090 is not accessible"
    echo "   Make sure processor service gRPC server is running"
    exit 1
fi

echo "✓ gRPC port 9090 is accessible"
echo ""

echo "2. Listing available gRPC services..."
echo "--------------------------------------"
grpcurl -plaintext localhost:9090 list

echo ""

echo "3. Describing ProcessorService..."
echo "--------------------------------------"
grpcurl -plaintext localhost:9090 describe processor.ProcessorService

echo ""

echo "4. Calling ProcessData method directly via gRPC..."
echo "--------------------------------------"
echo "Request: {\"message\": \"test-grpc-request\", \"correlationId\": \"test-123\"}"
echo ""

response=$(grpcurl -plaintext -d '{"message": "test-grpc-request", "correlationId": "test-123"}' \
    localhost:9090 processor.ProcessorService/ProcessData)

if [ $? -eq 0 ]; then
    echo "Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    echo ""
    echo "✓ gRPC call successful!"
else
    echo "❌ gRPC call failed"
    exit 1
fi

echo ""
echo "=========================================="
echo "Processor Service gRPC Tests Passed! ✓"
echo "=========================================="
echo ""
echo "Summary:"
echo "- Processor service gRPC server is running on port 9090"
echo "- ProcessorService.ProcessData method is accessible"
echo "- Direct gRPC communication works correctly"
