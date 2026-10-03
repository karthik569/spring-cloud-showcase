package com.example.gateway;

import io.micrometer.context.ContextRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "management.prometheus.metrics.export.enabled=true"
})
@AutoConfigureWebTestClient
class GatewayObservabilityTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired(required = false)
    private Tracer tracer;

    @Autowired(required = false)
    private ObservationRegistry observationRegistry;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Test
    @DisplayName("Tier 1: Prometheus endpoint returns 200 OK and valid Prometheus format")
    void testPrometheusEndpointReturns200AndValidFormat() {
        webTestClient.get()
                .uri("/actuator/prometheus")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertNotNull(body, "Prometheus response body must not be null");
                    assertTrue(body.contains("jvm_memory_used_bytes"),
                            "Body must contain jvm_memory_used_bytes");
                    assertTrue(body.contains("process_cpu_usage"),
                            "Body must contain process_cpu_usage");
                });
    }

    @Test
    @DisplayName("Tier 1: Prometheus exposition contains metadata HELP and TYPE definitions")
    void testPrometheusContainsExpositionHelpAndType() {
        webTestClient.get()
                .uri("/actuator/prometheus")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertNotNull(body);
                    assertTrue(body.contains("# HELP"), "Must contain Prometheus '# HELP' descriptors");
                    assertTrue(body.contains("# TYPE"), "Must contain Prometheus '# TYPE' descriptors");
                });
    }

    @Test
    @DisplayName("Tier 1: Gateway route observability endpoint reports configured routes")
    void testGatewayRoutesActuatorEndpoint() {
        webTestClient.get()
                .uri("/actuator/gateway/routes")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[?(@.route_id == 'order-service-route')]").exists()
                .jsonPath("$[?(@.route_id == 'inventory-service-route')]").exists();
    }

    @Test
    @DisplayName("Tier 2: Tracing and observability infrastructure beans are present")
    void testGatewayTracerAndObservationBeansPresent() {
        assertNotNull(tracer, "Tracer bean must be registered in Gateway application context");
        assertNotNull(observationRegistry, "ObservationRegistry bean must be registered");
        assertNotNull(meterRegistry, "MeterRegistry bean must be registered");
    }

    @Test
    @DisplayName("Tier 2: Reactive context propagation registry is active")
    void testReactorAutomaticContextPropagationAndRegistry() {
        ContextRegistry contextRegistry = ContextRegistry.getInstance();
        assertNotNull(contextRegistry, "ContextRegistry singleton must be available");
        // Verify ThreadLocal accessors are registered in ContextRegistry
        assertFalse(contextRegistry.getThreadLocalAccessors().isEmpty(),
                "ContextRegistry should have accessors registered for reactive propagation");
    }

    @Test
    @DisplayName("Tier 2: Inbound W3C traceparent header is accepted and processed by Gateway")
    void testInboundW3CTraceparentHeaderHandled() {
        String testTraceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        String testSpanId = "00f067aa0ba902b7";
        String traceparent = "00-" + testTraceId + "-" + testSpanId + "-01";

        webTestClient.get()
                .uri("/fallback/orders")
                .header("traceparent", traceparent)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.status").isEqualTo("FALLBACK_TRIGGERED")
                .jsonPath("$.message").isNotEmpty();
    }

    @Test
    @DisplayName("Tier 2: Inbound B3 headers are accepted and processed by Gateway")
    void testInboundB3HeaderHandled() {
        webTestClient.get()
                .uri("/fallback/orders")
                .header("X-B3-TraceId", "a1b2c3d4e5f60718")
                .header("X-B3-SpanId", "1234567890abcdef")
                .header("X-B3-Sampled", "1")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.status").isEqualTo("FALLBACK_TRIGGERED");
    }

    @Test
    @DisplayName("Tier 3: Standard Actuator metrics endpoint coexists with Prometheus")
    void testActuatorMetricsEndpointCoexists() {
        webTestClient.get()
                .uri("/actuator/metrics")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.names").isArray()
                .jsonPath("$.names[?(@ == 'jvm.memory.used')]").exists();
    }

    @Test
    @DisplayName("Tier 3: Actuator health endpoint coexists cleanly with Prometheus and tracing")
    void testActuatorHealthAndPrometheusCoexistence() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }
}
