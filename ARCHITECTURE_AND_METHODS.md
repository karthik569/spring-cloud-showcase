# Spring Cloud Microservices - In-Depth Architecture & Method Guide 🚀

This document provides a comprehensive, method-by-method technical deep dive into `spring-cloud-showcase`. It details the multi-module microservices architecture, Eureka service discovery, Spring Cloud Config, API Gateway routing, OpenFeign RPC with client-side load balancing, and Resilience4j circuit breakers.

---

## 1. Project Overview & Multi-Module Architecture

The showcase consists of 5 coordinated microservices built on Spring Cloud 2023.0.3:
1. **`config-server` (Port 8888)**: Serves externalized configuration properties from a native repository.
2. **`eureka-server` (Port 8761)**: Service registry managing discovery and heartbeat health checks.
3. **`api-gateway` (Port 8080)**: Reactive Spring Cloud Gateway performing path routing (`lb://`) and global logging.
4. **`inventory-service` (Port 8082)**: Microservice managing warehouse stock counts.
5. **`order-service` (Port 8081)**: Microservice orchestrating order placement via OpenFeign and Resilience4j.

```
                           +-------------------------------+
                           |     Spring Cloud Config       |
                           |        (Port: 8888)           |
                           +---------------+---------------+
                                           │
                                           ▼
 +------------------+         +-------------------------------+         +---------------------+
 │  HTTP Client /   │ ------> │      Spring Cloud Gateway     │ ------> │    Eureka Server    │
 │   Browser / cURL │         │        (Port: 8080)           │ <-----> │     (Port: 8761)    │
 +------------------+         +---------------+---------------+         +----------+----------+
                                              │                                    │
                              +---------------+---------------+                    │
                              │                               │                    │
                              ▼                               ▼                    ▼
               +-------------------------------+  OpenFeign  +-------------------------------+
               │         Order Service         │ ──────────► │       Inventory Service       │
               │          (Port: 8081)         │ (LoadBal.)  │          (Port: 8082)         │
               +-------------------------------+             +-------------------------------+
```

---

## 2. In-Depth Class & Method Breakdown

### A. Order Service Module (`order-service`)

#### [`OrderService.java`](file:///sdcard/Download/termux/spring-cloud-showcase/order-service/src/main/java/com/example/orderservice/service/OrderService.java)
- **`OrderResponse placeOrder(OrderRequest request)`**:
  - *Annotation*: `@CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackPlaceOrder")`.
  - *Operation*: Calls declarative OpenFeign client `inventoryClient.checkStock(request.skuCode())`. If stock is available, retrieves centralized discount percentage from Spring Cloud Config (`@Value("${order.discount-percentage:10}")`), computes total price, and generates a confirmed `OrderResponse`.
- **`OrderResponse fallbackPlaceOrder(OrderRequest request, Throwable ex)`**:
  - *Circuit Breaker Fallback*: Executed when `inventory-service` is unreachable or times out. Returns a graceful `REJECTED_OUT_OF_STOCK` response with note `"Insufficient stock or inventory service fallback triggered"`.

#### [`InventoryClient.java`](file:///sdcard/Download/termux/spring-cloud-showcase/order-service/src/main/java/com/example/orderservice/client/InventoryClient.java)
- **`InventoryResponse checkStock(@PathVariable("skuCode") String skuCode)`**:
  - *Annotation*: `@FeignClient(name = "inventory-service", fallback = InventoryFallback.class)`.
  - *Operation*: Automatically discovers `inventory-service` instances from Eureka, performs client-side load balancing via Spring Cloud LoadBalancer, and executes the remote HTTP call.

---

### B. Inventory Service Module (`inventory-service`)

#### [`InventoryController.java`](file:///sdcard/Download/termux/spring-cloud-showcase/inventory-service/src/main/java/com/example/inventoryservice/controller/InventoryController.java)
- **`InventoryResponse getStock(@PathVariable String skuCode)`**:
  - *Endpoint*: `GET /api/inventory/{skuCode}`.
  - *Operation*: Queries real-time stock from in-memory warehouse repository (`skuDatabase.get(skuCode)`). Returns stock availability and warehouse zone.

---

### C. API Gateway Module (`api-gateway`)

#### [`LoggingGlobalFilter.java`](file:///sdcard/Download/termux/spring-cloud-showcase/api-gateway/src/main/java/com/example/apigateway/filter/LoggingGlobalFilter.java)
- **`Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)`**:
  - *Operation*: Global Netty reactive filter intercepting every inbound request. Logs request URI, HTTP method, client IP, and forwards to target downstream microservice via Eureka virtual URI (`lb://order-service` or `lb://inventory-service`).

#### [`FallbackController.java`](file:///sdcard/Download/termux/spring-cloud-showcase/api-gateway/src/main/java/com/example/apigateway/controller/FallbackController.java)
- **`Mono<ResponseEntity<Map<String, String>>> orderFallback()`**: Gateway-level fallback responding with HTTP 503 when the order service route trips the gateway circuit breaker.
