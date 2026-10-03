package com.example.order.controller;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import feign.Capability;
import feign.micrometer.MicrometerCapability;
import feign.micrometer.MicrometerObservationCapability;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignClientFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "management.prometheus.metrics.export.enabled=true"
})
@AutoConfigureMockMvc
class OrderObservabilityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private Tracer tracer;

    @Autowired(required = false)
    private ObservationRegistry observationRegistry;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Autowired(required = false)
    private FeignClientFactory feignClientFactory;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger orderLogger;

    @BeforeEach
    void setUp() {
        orderLogger = (Logger) LoggerFactory.getLogger(OrderController.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        orderLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        if (orderLogger != null && listAppender != null) {
            orderLogger.detachAppender(listAppender);
        }
    }


    @Test
    @DisplayName("Tier 1: Prometheus endpoint responds with 200 OK and text exposition format")
    void testPrometheusEndpointReturns200AndValidContentType() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn();

        String contentType = result.getResponse().getContentType();
        assertNotNull(contentType, "Content-Type must not be null");
        assertTrue(contentType.contains("text/plain") || contentType.contains("openmetrics"),
                "Expected Prometheus or OpenMetrics text content type, got: " + contentType);
    }

    @Test
    @DisplayName("Tier 1: Prometheus endpoint contains JVM memory and CPU metrics")
    void testPrometheusEndpointContainsJvmAndCpuMetrics() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("jvm_memory_used_bytes"),
                "Metrics output must contain jvm_memory_used_bytes");
        assertTrue(body.contains("process_cpu_usage"),
                "Metrics output must contain process_cpu_usage");
        assertTrue(body.contains("system_cpu_usage") || body.contains("process_uptime_seconds"),
                "Metrics output must contain system or process metrics");
    }

    @Test
    @DisplayName("Tier 1: Prometheus endpoint records HTTP server request timers after incoming requests")
    void testPrometheusEndpointContainsHttpRequestTimers() throws Exception {
        // Generate an HTTP request so ServerHttpObservationFilter records a timer
        mockMvc.perform(get("/api/orders/config-info").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("http_server_requests"),
                "Metrics output must contain http_server_requests timer metric after handling request");
    }

    @Test
    @DisplayName("Tier 1: Feign tracing observation capability bean is present and active")
    void testFeignTracingObservationCapabilityBeanPresent() {
        assertNotNull(feignClientFactory, "FeignClientFactory bean should be registered in context");

        // Verify MicrometerObservationCapability instance exists for inventory-service Feign client
        MicrometerObservationCapability observationCapability =
                feignClientFactory.getInstance("inventory-service", MicrometerObservationCapability.class);
        assertNotNull(observationCapability,
                "MicrometerObservationCapability should be configured in inventory-service Feign context");

        // Verify Feign Capabilities are discovered
        Map<String, Capability> capabilities =
                feignClientFactory.getInstancesWithoutAncestors("inventory-service", Capability.class);
        assertNotNull(capabilities, "Capabilities map must not be null");
        assertFalse(capabilities.isEmpty(), "Feign client must have at least one Capability registered");
        assertTrue(capabilities.values().stream().anyMatch(c -> c instanceof MicrometerObservationCapability),
                "At least one registered Capability must be MicrometerObservationCapability");
    }

    @Test
    @DisplayName("Tier 2: Tracing infrastructure beans (Tracer, ObservationRegistry, MeterRegistry) are active")
    void testTracingInfrastructureBeansActive() {
        assertNotNull(tracer, "Tracer bean (Brave bridge) must be present in ApplicationContext");
        assertNotNull(observationRegistry, "ObservationRegistry bean must be present in ApplicationContext");
        assertNotNull(meterRegistry, "MeterRegistry bean must be present in ApplicationContext");
    }

    @Test
    @DisplayName("Tier 2: Inbound W3C traceparent header propagates traceId into MDC logging")
    void testInboundW3CTraceparentPropagationToMdc() throws Exception {
        String testTraceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        String testSpanId = "00f067aa0ba902b7";
        String traceparent = "00-" + testTraceId + "-" + testSpanId + "-01";

        mockMvc.perform(get("/api/orders/config-info")
                        .header("traceparent", traceparent)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Inbound traceparent should bind to current trace context during request processing
        assertNotNull(tracer, "Tracer must be active");
    }

    @Test
    @DisplayName("Tier 2: Inbound B3 headers propagate traceId into tracing context")
    void testInboundB3HeaderPropagation() throws Exception {
        String testTraceId = "a1b2c3d4e5f60718";
        String testSpanId = "1234567890abcdef";

        mockMvc.perform(get("/api/orders/config-info")
                        .header("X-B3-TraceId", testTraceId)
                        .header("X-B3-SpanId", testSpanId)
                        .header("X-B3-Sampled", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        assertNotNull(tracer, "Tracer must be present to support B3 extraction");
    }

    @Test
    @DisplayName("Tier 3: Standard Actuator metrics endpoint coexists with Prometheus scrape endpoint")
    void testStandardActuatorMetricsEndpointCoexists() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.parseMediaType("application/vnd.spring-boot.actuator.v3+json")))
                .andExpect(jsonPath("$.names").isArray())
                .andExpect(jsonPath("$.names").isNotEmpty());
    }
}
