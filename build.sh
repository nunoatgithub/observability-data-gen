#!/bin/bash

set -e

echo "Building all services..."
mvn clean package -DskipTests

echo ""
echo "Building Docker images..."

echo "Building processor-service image..."
cd processor-service
docker build -t observability/processor-service:1.0.0 .
cd ..

echo "Building gateway-service image..."
cd gateway-service
docker build -t observability/gateway-service:1.0.0 .
cd ..

echo ""
echo "Build complete! Images created:"
docker images | grep observability

echo ""
echo "To run the services, execute: docker-compose up"
