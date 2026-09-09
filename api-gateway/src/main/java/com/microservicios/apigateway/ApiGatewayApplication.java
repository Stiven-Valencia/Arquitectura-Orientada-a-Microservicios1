package com.microservicios.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del API Gateway.
 *
 * Cumple dos funciones en esta arquitectura:
 *  1. Enruta las peticiones /api/** al microservicio que corresponda, de modo
 *     que el cliente solo necesita conocer un puerto (8080) en lugar de dos.
 *  2. Sirve la interfaz web estatica desde el mismo origen, lo que elimina
 *     el problema de CORS sin tener que relajar la seguridad de los servicios.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
