#!/bin/bash

# Wait for LocalStack to be ready
sleep 5

echo "Creating CloudWatch Log Groups..."

# Create log groups
awslocal logs create-log-group --log-group-name "producto-log-group" 2>/dev/null || true
awslocal logs create-log-group --log-group-name "ordenes-log-group" 2>/dev/null || true
awslocal logs create-log-group --log-group-name "pagos-log-group" 2>/dev/null || true
awslocal logs create-log-group --log-group-name "apigateway-log-group" 2>/dev/null || true
awslocal logs create-log-group --log-group-name "eureka-log-group" 2>/dev/null || true

# Create log streams
awslocal logs create-log-stream --log-group-name "producto-log-group" --log-stream-name "producto-service-stream" 2>/dev/null || true
awslocal logs create-log-stream --log-group-name "ordenes-log-group" --log-stream-name "ordenes-service-stream" 2>/dev/null || true
awslocal logs create-log-stream --log-group-name "pagos-log-group" --log-stream-name "pagos-service-stream" 2>/dev/null || true
awslocal logs create-log-stream --log-group-name "apigateway-log-group" --log-stream-name "apigateway-stream" 2>/dev/null || true
awslocal logs create-log-stream --log-group-name "eureka-log-group" --log-stream-name "eureka-server-stream" 2>/dev/null || true

echo "CloudWatch Log Groups and Streams created successfully!"
