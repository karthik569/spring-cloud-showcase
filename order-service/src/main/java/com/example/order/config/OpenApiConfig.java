package com.example.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API")
                        .description("REST API for creating and managing orders, checking inventory via OpenFeign, and testing Resilience4j Circuit Breakers in the Spring Cloud Showcase.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Spring Cloud Showcase Team")
                                .email("dev@example.com")
                                .url("https://spring.io"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server().url("/").description("Direct or Gateway Routed URL")
                ));
    }
}
