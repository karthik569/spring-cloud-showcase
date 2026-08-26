# Spring Cloud Microservices Showcase 🚀

A comprehensive, production-grade Spring Cloud microservices architecture built with **Java 21**, **Spring Boot 3.3**, and **Spring Cloud 2023.0.3 (Leyton)**.

---

## 🏛 Architecture Overview

```
                          +-------------------------------+
                          |     Spring Cloud Config       |
                          |        (Port: 8888)           |
                          +---------------+---------------+
                                          | (Centralized Config)
                                          v
+------------------+         +-------------------------------+         +---------------------+
|  HTTP Client /   | ------> |      Spring Cloud Gateway     | ------> |    Eureka Server    |
|   Browser / cURL |         |        (Port: 8080)           | <-----> |     (Port: 8761)    |
+------------------+         +---------------+---------------+         +----------+----------+
                                             |                                    |
                             +---------------+---------------+                    | (Service Discovery)
                             |                               |                    |
                             v                               v                    v
              +-------------------------------+  OpenFeign  +-------------------------------+
              |         Order Service         | ----------> |       Inventory Service       |
              |          (Port: 8081)         | (LoadBal.)  |          (Port: 8082)         |
              +-------------------------------+             +-------------------------------+
              | - Feign Client & Fallbacks    |             | - @RefreshScope               |
              | - Resilience4j Circuit Breaker|             | - Dynamic In-Memory Stock     |
              | - @RefreshScope Dynamic Config|             +-------------------------------+
              +-------------------------------+
```

---

## 🌟 Comprehensive Method-by-Method Breakdown

### 1. `order-service` -> `OrderService.java`
- **`OrderResponse placeOrder(OrderRequest request)`**: Validates request parameters, calls `InventoryClient.checkStock(skuCode)` via OpenFeign with client-side load balancing, applies centralized discount percentages retrieved dynamically from Spring Cloud Config, and creates a confirmed order.
- **`OrderResponse fallbackPlaceOrder(OrderRequest request, Throwable ex)`**: Circuit breaker fallback activated when `inventory-service` is down or slow, returning a graceful `REJECTED_OUT_OF_STOCK` response without crashing.

### 2. `order-service` -> `InventoryClient.java`
- **`InventoryResponse checkStock(@PathVariable("skuCode") String skuCode)`**: Declarative OpenFeign interface binding to `http://inventory-service/api/inventory/{skuCode}` with Eureka name resolution and fallback class `InventoryFallback.class`.

### 3. `inventory-service` -> `InventoryController.java`
- **`InventoryResponse getStock(@PathVariable String skuCode)`**: Looks up real-time stock counts from the in-memory warehouse repository and returns availability status.

### 4. `api-gateway` -> `LoggingGlobalFilter.java`
- **`Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)`**: Reactive Netty filter intercepting all inbound HTTP traffic, logging incoming URI, IP origin, and request timestamp before routing to target microservices.

### 5. `api-gateway` -> `FallbackController.java`
- **`Mono<ResponseEntity<Map<String, String>>> orderFallback()`**: Circuit breaker fallback handler returning HTTP 503 when downstream services are unreachable.

---

## 📦 Project Modules

1. **[`config-server`](file:///sdcard/Download/termux/spring-cloud-showcase/config-server)**: Port `8888`
2. **[`eureka-server`](file:///sdcard/Download/termux/spring-cloud-showcase/eureka-server)**: Port `8761`
3. **[`api-gateway`](file:///sdcard/Download/termux/spring-cloud-showcase/api-gateway)**: Port `8080`
4. **[`inventory-service`](file:///sdcard/Download/termux/spring-cloud-showcase/inventory-service)**: Port `8082`
5. **[`order-service`](file:///sdcard/Download/termux/spring-cloud-showcase/order-service)**: Port `8081`

---

## 🚀 How to Run in Termux

### Step 1: Build the Entire Multi-Module Project
```bash
cd /sdcard/Download/termux/spring-cloud-showcase
mvn clean package -DskipTests
```

### Step 2: Start the Services in Sequence

#### 1️⃣ Config Server (Port 8888)
```bash
mvn -pl config-server spring-boot:run
```

#### 2️⃣ Eureka Server (Port 8761)
```bash
mvn -pl eureka-server spring-boot:run
```

#### 3️⃣ Inventory Service (Port 8082)
```bash
mvn -pl inventory-service spring-boot:run
```

#### 4️⃣ Order Service (Port 8081)
```bash
mvn -pl order-service spring-boot:run
```

#### 5️⃣ API Gateway (Port 8080)
```bash
mvn -pl api-gateway spring-boot:run
```

---

## 🧪 Interactive API Testing via Gateway

### 1. Check Inventory via API Gateway
```bash
curl -i http://localhost:8080/api/inventory/IPHONE15
```

---

### 2. Place Order via API Gateway
```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"skuCode":"IPHONE15","quantity":2,"price":1000.0}'
```
