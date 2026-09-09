package com.microservicios.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del Microservicio de Usuarios.
 *
 * Este servicio es la fuente de verdad de los datos de usuario y expone
 * su informacion mediante una API REST en el puerto 8081. No conoce ni
 * depende de ningun otro microservicio del sistema.
 */
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
