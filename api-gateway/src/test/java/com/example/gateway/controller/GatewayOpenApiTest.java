package com.example.gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false"
})
@AutoConfigureWebTestClient
class GatewayOpenApiTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testSwaggerUiHtmlRedirectOrOk() {
        webTestClient.get()
                .uri("/swagger-ui.html")
                .exchange()
                .expectStatus().is3xxRedirection();
    }

    @Test
    void testSwaggerUiIndexHtml() {
        webTestClient.get()
                .uri("/webjars/swagger-ui/index.html")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testSwaggerConfigEndpoint() {
        webTestClient.get()
                .uri("/v3/api-docs/swagger-config")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.urls").isArray()
                .jsonPath("$.urls[?(@.name == 'Order Service')].url").isEqualTo("/order-service/v3/api-docs")
                .jsonPath("$.urls[?(@.name == 'Inventory Service')].url").isEqualTo("/inventory-service/v3/api-docs");
    }
}
