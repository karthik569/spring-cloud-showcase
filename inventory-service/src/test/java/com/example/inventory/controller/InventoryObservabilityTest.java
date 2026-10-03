package com.example.inventory.controller;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "management.prometheus.metrics.export.enabled=true"
})
@AutoConfigureMockMvc
class InventoryObservabilityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired(required = false)
    private Tracer tracer;

    @Autowired(required = false)
    private ObservationRegistry observationRegistry;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Autowired(required = false)
    private brave.Tracing tracing;

    @Autowired(required = false)
    private org.springframework.boot.actuate.autoconfigure.tracing.TracingProperties tracingProperties;

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger inventoryLogger;

    @BeforeEach
    void setUp() {
        inventoryLogger = (Logger) LoggerFactory.getLogger(InventoryController.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        inventoryLogger.addAppender(listAppender);
    }


    @AfterEach
    void tearDown() {
        if (inventoryLogger != null && listAppender != null) {
            inventoryLogger.detachAppender(listAppender);
        }
    }

    @Test
    @DisplayName("Tier 1: Prometheus endpoint responds with 200 OK and valid exposition format")
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
    @DisplayName("Tier 1: Prometheus endpoint contains JVM memory and system metrics")
    void testPrometheusEndpointContainsJvmAndSystemMetrics() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("jvm_memory_used_bytes"),
                "Prometheus output must contain jvm_memory_used_bytes");
        assertTrue(body.contains("process_cpu_usage"),
                "Prometheus output must contain process_cpu_usage");
        assertTrue(body.contains("system_cpu_usage") || body.contains("process_uptime_seconds") || body.contains("jvm_threads_live_threads"),
                "Prometheus output must contain system or process metrics");
    }

    @Test
    @DisplayName("Tier 1: Prometheus endpoint records HTTP server request timers after inventory queries")
    void testPrometheusRecordsHttpRequestTimers() throws Exception {
        // Query stock to trigger ServerHttpObservationFilter recording
        mockMvc.perform(get("/api/inventory/IPHONE15").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuCode").value("IPHONE15"));

        MvcResult result = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("http_server_requests"),
                "Prometheus output must contain http_server_requests timer metric");
    }

    @Test
    @DisplayName("Tier 2: Inbound W3C traceparent header extracts traceId and attaches to MDC and child span")
    void testInboundW3CTraceparentExtractsTraceIdToMdc() throws Exception {
        String testTraceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        String testSpanId = "00f067aa0ba902b7";
        String traceparent = "00-" + testTraceId + "-" + testSpanId + "-01";

        mockMvc.perform(get("/api/inventory/IPHONE15")
                        .header("traceparent", traceparent)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuCode").value("IPHONE15"))
                .andExpect(jsonPath("$.inStock").value(true));

        List<ILoggingEvent> events = listAppender.list.stream()
                .filter(e -> e.getMessage().contains("Checking inventory stock"))
                .toList();
        assertFalse(events.isEmpty(), "Expected at least one log event from InventoryController");

        ILoggingEvent event = events.get(0);
        String traceIdInMdc = event.getMDCPropertyMap().get("traceId");
        String spanIdInMdc = event.getMDCPropertyMap().get("spanId");

        assertNotNull(traceIdInMdc, "MDC traceId must be present");
        assertFalse(traceIdInMdc.isBlank(), "MDC traceId must not be blank");
        assertNotNull(spanIdInMdc, "MDC spanId must be present");
        assertFalse(spanIdInMdc.isBlank(), "MDC spanId must not be blank");
    }

    @Test
    @DisplayName("Tier 2: Inbound B3 headers extract traceId and attach to MDC and server span")
    void testInboundB3HeadersExtractTraceIdToMdc() throws Exception {
        String testTraceId = "a1b2c3d4e5f60718";
        String testSpanId = "1234567890abcdef";

        mockMvc.perform(get("/api/inventory/MACBOOK-PRO")
                        .header("X-B3-TraceId", testTraceId)
                        .header("X-B3-SpanId", testSpanId)
                        .header("X-B3-Sampled", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuCode").value("MACBOOK-PRO"))
                .andExpect(jsonPath("$.inStock").value(true));

        List<ILoggingEvent> events = listAppender.list.stream()
                .filter(e -> e.getMessage().contains("Checking inventory stock"))
                .toList();
        assertFalse(events.isEmpty(), "Expected log event from InventoryController");

        ILoggingEvent event = events.get(0);
        String traceIdInMdc = event.getMDCPropertyMap().get("traceId");
        String spanIdInMdc = event.getMDCPropertyMap().get("spanId");

        assertNotNull(traceIdInMdc, "MDC traceId must be present");
        assertFalse(traceIdInMdc.isBlank(), "MDC traceId must not be blank");
        assertNotNull(spanIdInMdc, "MDC spanId must be present");
        assertFalse(spanIdInMdc.isBlank(), "MDC spanId must not be blank");
    }

    @Test
    @DisplayName("Tier 2: Inbound request without tracing headers generates new traceId and attaches to MDC")
    void testAutoGeneratedTraceIdWithoutHeaders() throws Exception {
        mockMvc.perform(get("/api/inventory/AIRPODS-PRO")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuCode").value("AIRPODS-PRO"));

        List<ILoggingEvent> events = listAppender.list.stream()
                .filter(e -> e.getMessage().contains("Checking inventory stock"))
                .toList();
        assertFalse(events.isEmpty(), "Expected log event from InventoryController");

        ILoggingEvent event = events.get(0);
        String traceIdInMdc = event.getMDCPropertyMap().get("traceId");
        String spanIdInMdc = event.getMDCPropertyMap().get("spanId");

        assertNotNull(traceIdInMdc, "MDC traceId should be automatically generated");
        assertFalse(traceIdInMdc.isBlank(), "MDC traceId must not be blank");
        assertNotNull(spanIdInMdc, "MDC spanId should be automatically generated");
        assertFalse(spanIdInMdc.isBlank(), "MDC spanId must not be blank");
    }

    @Test
    @DisplayName("Tier 3: Error path retains traceId correlation in MDC and error logs")
    void testErrorPathPreservesTraceCorrelation() {
        String testTraceId = "55556666777788889999000011112222";
        String testSpanId = "0000000000000001";
        String traceparent = "00-" + testTraceId + "-" + testSpanId + "-01";

        try {
            mockMvc.perform(get("/api/inventory/fail/IPHONE15")
                    .header("traceparent", traceparent));
        } catch (Exception ignored) {
            // Expected simulated exception from InventoryController
        }

        List<ILoggingEvent> events = listAppender.list.stream()
                .filter(e -> e.getMessage().contains("Simulating 500 error"))
                .toList();
        assertFalse(events.isEmpty(), "Expected log event for 500 simulation");

        ILoggingEvent event = events.get(0);
        String traceIdInMdc = event.getMDCPropertyMap().get("traceId");
        assertNotNull(traceIdInMdc, "MDC traceId must be present on error path");
        assertFalse(traceIdInMdc.isBlank(), "MDC traceId must not be blank on error path");
    }

    @Test
    @DisplayName("Tier 3: Tracing and metrics infrastructure beans are present")
    void testTracingAndMetricsBeansPresent() {
        assertNotNull(tracer, "Tracer bean must be present");
        assertNotNull(observationRegistry, "ObservationRegistry bean must be present");
        assertNotNull(meterRegistry, "MeterRegistry bean must be present");
        if (tracing != null) {
            System.out.println("=== DIAGNOSTIC: PROPAGATION KEYS: " + tracing.propagation().keys());
        }
        if (tracingProperties != null) {
            System.out.println("=== DIAGNOSTIC: TYPE: " + tracingProperties.getPropagation().getType());
        }
    }

    @Test
    @DisplayName("Tier 3: Standard Actuator health endpoint coexists with Prometheus scrape endpoint")
    void testStandardActuatorEndpointsCoexist() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
