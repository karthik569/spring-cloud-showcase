# 🚀 Getting Started with Spring Cloud Showcase

Welcome to the **Spring Cloud Showcase**! This repository provides an end-to-end, production-grade microservices system built with **Java 21**, **Spring Boot 3.3.4**, and **Spring Cloud 2023.0.3 (Leyton)**.

Whether you are running on a local desktop Linux/macOS/Windows environment or on Android Termux (PRoot Ubuntu), this guide will get you up and running quickly.

---

## 📋 Prerequisites

Before running the project, verify that the following tools are installed:

- **Java JDK 21+** (`java -version`)
- **Apache Maven 3.9+** (`mvn -v`)
- **cURL** and **jq** (recommended for terminal API testing)
- Optional: **Docker** (if running standalone Zipkin server `openzipkin/zipkin`)

---

## 🏛 System Architecture at a Glance

The showcase is organized as a multi-module Maven project with 5 microservices:

| Module | Port | Role & Capabilities |
|:---|:---:|:---|
| **[`config-server`](file:///config-server)** | `8888` | Centralized externalized configuration management (native profile fallback) |
| **[`eureka-server`](file:///eureka-server)** | `8761` | Netflix Eureka service discovery & dynamic instance registry |
| **[`api-gateway`](file:///api-gateway)** | `8080` | Spring Cloud Gateway (reactive WebFlux, route filtering, Swagger aggregation, context propagation) |
| **[`order-service`](file:///order-service)** | `8081` | Order processing with OpenFeign client, Resilience4j circuit breakers, and micrometer tracing |
| **[`inventory-service`](file:///inventory-service)** | `8082` | Real-time warehouse inventory management with dynamic `@RefreshScope` |

```
                              +--------------------+
                              |   Config Server    |
                              |    (Port 8888)     |
                              +---------+----------+
                                        | (Dynamic Config)
                                        v
+--------------------+        +--------------------+        +--------------------+
|  Client / Browser  | -----> |    API Gateway     | -----> |   Eureka Server    |
|   (HTTP Requests)  |        |    (Port 8080)     | <----> |    (Port 8761)     |
+--------------------+        +---------+----------+        +---------+----------+
                                        |                             |
                       +----------------+----------------+            | (Discovery)
                       |                                 |            |
                       v                                 v            v
            +--------------------+   OpenFeign  +--------------------+
            |   Order Service    | -----------> | Inventory Service  |
            |    (Port 8081)     | (LoadBal.)   |    (Port 8082)     |
            +--------------------+              +--------------------+
            | - Circuit Breaker  |              | - Dynamic Stock    |
            | - Micrometer Trace |              | - Metrics Scrape   |
            +--------------------+              +--------------------+
```

---

## 🛠️ Step 1: Building the Project

Run Maven clean package from the root directory:

```bash
# Clean and package all modules
mvn clean package -DskipTests
```

To run all automated unit, integration, and observability tests:

```bash
mvn test
```

---

## 🚦 Step 2: Starting the Services (Order Matters!)

Because microservices depend on configuration and service registry, launch them in the following order:

### 1️⃣ Start Config Server (Port 8888)
```bash
mvn -pl config-server spring-boot:run
```
*Wait ~10 seconds until it logs that it has started on port 8888.*

### 2️⃣ Start Eureka Server (Port 8761)
```bash
mvn -pl eureka-server spring-boot:run
```
*Access the Eureka Dashboard at: [http://localhost:8761](http://localhost:8761)*

### 3️⃣ Start Inventory Service (Port 8082)
```bash
mvn -pl inventory-service spring-boot:run
```

### 4️⃣ Start Order Service (Port 8081)
```bash
mvn -pl order-service spring-boot:run
```

### 5️⃣ Start API Gateway (Port 8080)
```bash
mvn -pl api-gateway spring-boot:run
```

---

## 🔍 Step 3: Verifying the System

### 1. Check Service Discovery
Open the Eureka Web Dashboard:
👉 **[http://localhost:8761](http://localhost:8761)**

Ensure that `API-GATEWAY`, `ORDER-SERVICE`, and `INVENTORY-SERVICE` appear under **Instances currently registered with Eureka**.

---

### 2. Test API Endpoints via Gateway

#### Check Inventory Stock:
```bash
curl -i http://localhost:8080/api/inventory/IPHONE15
```
**Sample Response:**
```json
{
  "skuCode": "IPHONE15",
  "inStock": true,
  "quantity": 25,
  "warehouse": "Default-Warehouse"
}
```

#### Place an Order:
```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"skuCode":"IPHONE15","quantity":2,"price":1000.0}'
```
**Sample Response:**
```json
{
  "orderNumber": "ORD-XXXXXXXX",
  "skuCode": "IPHONE15",
  "quantity": 2,
  "totalPrice": 2000.0,
  "status": "CONFIRMED",
  "message": "Order placed successfully! Inventory reserved."
}
```

#### Test Resilience4j Circuit Breaker Fallback:
Trigger simulated inventory failure or downstate:
```bash
curl -i -X POST "http://localhost:8080/api/orders/simulate-circuit-breaker?skuCode=IPHONE15&mode=fail"
```
Notice the graceful fallback response rather than a broken HTTP 500.

---

### 3. Interactive Swagger & OpenAPI Documentation

Every service and route provides OpenAPI documentation and Swagger UI:

| Component | Swagger UI URL | OpenAPI v3 JSON Spec |
|:---|:---|:---|
| **API Gateway (Aggregated UI)** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | `http://localhost:8080/v3/api-docs/swagger-config` |
| **Order Service** | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) | `http://localhost:8081/v3/api-docs` |
| **Inventory Service** | [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html) | `http://localhost:8082/v3/api-docs` |

---

### 4. Metrics & Distributed Tracing (Observability)

All microservices are equipped with **Micrometer Tracing** (Brave bridge) and **Prometheus metrics**:

#### Prometheus Metrics Endpoints:
```bash
curl -s http://localhost:8080/actuator/prometheus | grep "jvm_memory_used_bytes"
curl -s http://localhost:8081/actuator/prometheus | grep "http_server_requests"
curl -s http://localhost:8082/actuator/prometheus | grep "process_cpu_usage"
```

#### Correlation Logging:
All application log output automatically includes correlation IDs:
```text
2026-10-03 08:46:10.127 [main] INFO [inventory-service,6ac07379407a65d8200a771db6d1b32a,200a771db6d1b32a] ...
                                     ^^^^^^^^^^^^^^^^^ ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ^^^^^^^^^^^^^^^^
                                     Service Name      Trace ID                         Span ID
```

---

## 🧪 Automated Test Suite (`test-app.sh`)

A test runner CLI script is included to quickly validate builds, OpenAPI documents, and observability:

```bash
# Run comprehensive test suite (Unit/Integration + OpenAPI + Observability + Logging)
bash test-app.sh --all

# Run distributed tracing & observability test suite
bash test-app.sh --observability

# Run Prometheus scrape validation
bash test-app.sh --prometheus

# Run distributed tracing propagation validation
bash test-app.sh --tracing

# Run Swagger / OpenAPI validation
bash test-app.sh --swagger

# Probe live running microservices over HTTP
bash test-app.sh --live
```

---

## 📁 Repository Directory Structure

```text
spring-cloud-showcase/
├── pom.xml                                   # Root aggregator POM & dependency management
├── GETTING_STARTED.md                        # Quick start guide (this document)
├── README.md                                 # Main project documentation & design breakdown
├── ARCHITECTURE_AND_METHODS.md               # Detailed class and method architectural reference
├── architecture-documentation.html           # Interactive visual architecture guide
├── test-app.sh                               # Unified CLI test & validation script
│
├── config-server/                            # Spring Cloud Config Server (Port 8888)
├── eureka-server/                            # Netflix Eureka Service Discovery (Port 8761)
├── api-gateway/                              # Reactive Spring Cloud Gateway (Port 8080)
├── inventory-service/                        # Inventory microservice (Port 8082)
└── order-service/                            # Order microservice (Port 8081)
```

---

## 💡 Troubleshooting & FAQ

<details>
<summary><b>1. Why does a service fail to connect on startup?</b></summary>
Ensure that <code>config-server</code> (8888) and <code>eureka-server</code> (8761) are fully started before starting downstream business services. Check logs in the <code>logs/</code> directory.
</details>

<details>
<summary><b>2. How can I change configurations without restarting services?</b></summary>
Configurations support Spring Cloud Config client refresh. After modifying a property, trigger a refresh POST request to the actuator:
<pre>curl -X POST http://localhost:8081/actuator/refresh</pre>
</details>

<details>
<summary><b>3. Where can I see distributed trace spans?</b></summary>
If you run Zipkin on port 9411 (<code>docker run -d -p 9411:9411 openzipkin/zipkin</code>), spans are automatically exported to <code>http://localhost:9411/api/v2/spans</code>.
</details>

---

Happy coding! If you have questions or want to extend this project, check out [`ARCHITECTURE_AND_METHODS.md`](file:///ARCHITECTURE_AND_METHODS.md).
