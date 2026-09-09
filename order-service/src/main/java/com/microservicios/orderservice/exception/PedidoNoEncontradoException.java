package com.microservicios.orderservice.exception;

/** Se lanza cuando se solicita un pedido que no existe en este servicio. */
public class PedidoNoEncontradoException extends RuntimeException {

    public PedidoNoEncontradoException(Long id) {
        super("No existe un pedido con id " + id);
    }
}
