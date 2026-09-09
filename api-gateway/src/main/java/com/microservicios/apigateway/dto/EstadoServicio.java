package com.microservicios.apigateway.dto;

/**
 * Estado de salud de un microservicio, tal como lo muestra el diagrama de la
 * interfaz web: verde si responde, rojo si esta caido.
 *
 * @param nombre     nombre legible del servicio
 * @param url        direccion base donde se le busca
 * @param disponible true si respondio a la sonda
 * @param latenciaMs milisegundos que tardo en responder
 */
public record EstadoServicio(String nombre, String url, boolean disponible, long latenciaMs) {
}
