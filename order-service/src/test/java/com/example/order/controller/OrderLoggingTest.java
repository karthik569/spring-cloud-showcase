package com.example.order.controller;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false"
})
class OrderLoggingTest {

    private static final Logger log = LoggerFactory.getLogger(OrderLoggingTest.class);

    @Test
    void testLoggingWritesToFile() throws Exception {
        String testToken = "TEST_LOG_TOKEN_" + UUID.randomUUID();
        log.info("Emitting test verification log entry: {}", testToken);

        File logFile = new File("logs/order-service.log");
        assertTrue(logFile.exists(), "Expected logs/order-service.log to be created");

        String content = Files.readString(logFile.toPath());
        assertTrue(content.contains(testToken), "Expected log file to contain emitted log entry token");
    }
}
