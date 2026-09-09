package com.microservicios.orderservice.dto;

import com.microservicios.orderservice.model.Pedido;

/**
 * Respuesta compuesta: el pedido propio de este servicio mas los datos del
 * usuario traidos por REST del otro microservicio.
 *
 * Es el resultado visible de la comunicacion entre servicios: ningun almacen
 * contiene esta estructura completa, se arma en tiempo de peticion.
 */
public record PedidoDetalleDTO(Pedido pedido, UsuarioDTO usuario) {
}
