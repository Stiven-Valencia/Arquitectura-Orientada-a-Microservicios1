package com.microservicios.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Copia local y de solo lectura del usuario que devuelve el microservicio
 * de Usuarios.
 *
 * Es un DTO y no la entidad original a proposito: cada microservicio define
 * su propia vista del dato y solo declara los campos que realmente consume.
 * @JsonIgnoreProperties evita que este servicio se rompa si el servicio de
 * Usuarios agrega campos nuevos a su respuesta (compatibilidad hacia atras).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsuarioDTO(Long id, String nombre, String correo, String ciudad) {
}
