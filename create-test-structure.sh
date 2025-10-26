#!/bin/bash

# Script to automatically create test structure for all services

echo "🎯 Creating test structure for all services..."

create_service_tests() {
    local service_name=$1
    local package_name=$2
    local test_path="$(pwd)/$service_name/src/test/java/$(echo $package_name | tr '.' '/')"

    echo "📁 Creating tests for $service_name..."

    # Create directory structure
    mkdir -p "$test_path/controller"
    mkdir -p "$test_path/service"
    mkdir -p "$test_path/integration"
    mkdir -p "$test_path/util"

    # Create basic test files
    cat > "$test_path/controller/${service_name%%-*}ControllerTest.java" << EOF
package $package_name.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ${service_name%%-*}ControllerTest {

    @Test
    void contextLoads() {
        assertTrue(true);
    }
}
EOF

    cat > "$test_path/service/${service_name%%-*}ServiceTest.java" << EOF
package $package_name.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ${service_name%%-*}ServiceTest {

    @Test
    void sampleTest() {
        assertTrue(true);
    }
}
EOF

    echo "✅ Created test structure for $service_name"
}

# Create tests for all services
create_service_tests "auth-service" "com.auth"
create_service_tests "booking-service" "com.hotelbooking.booking"
create_service_tests "hotel-service" "com.hotelbooking.hotel"
create_service_tests "api-gateway" "com.hotelbooking.gateway"
create_service_tests "eureka-server" "com.hotelbooking.eureka"

echo "🎉 Test structure created successfully!"
echo "📝 Remember to add test dependencies to each service's pom.xml"