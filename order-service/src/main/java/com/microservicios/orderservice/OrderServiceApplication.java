package com.microservicios.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del Microservicio de Pedidos.
 *
 * Corre en el puerto 8082 y es el servicio *consumidor* de la arquitectura:
 * para registrar o mostrar un pedido necesita datos que no le pertenecen
 * (los del usuario) y los obtiene llamando por HTTP al microservicio de
 * Usuarios. Esa llamada es la comunicacion REST que exige el ejercicio.
 */
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
