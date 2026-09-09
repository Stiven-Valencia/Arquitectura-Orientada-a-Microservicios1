package com.microservicios.orderservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configura el cliente HTTP que usa este servicio para hablar con otros.
 *
 * Los timeouts son deliberados: en una arquitectura distribuida la red falla,
 * y sin limite de espera una caida del servicio de Usuarios agotaria los hilos
 * del servicio de Pedidos y lo tumbaria tambien (fallo en cascada).
 */
@Configuration
public class RestTemplateConfig {

    @Value("${usuarios.service.timeout-conexion-ms}")
    private long timeoutConexionMs;

    @Value("${usuarios.service.timeout-lectura-ms}")
    private long timeoutLecturaMs;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofMillis(timeoutConexionMs))
                .readTimeout(Duration.ofMillis(timeoutLecturaMs))
                .build();
    }
}
