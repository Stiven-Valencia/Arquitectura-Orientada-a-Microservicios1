package com.microservicios.orderservice.exception;

/**
 * El microservicio de Usuarios respondio 404: el usuario referenciado por el
 * pedido no existe. Es un error del cliente (400/404), no una falla del sistema.
 */
public class UsuarioInexistenteException extends RuntimeException {

    public UsuarioInexistenteException(Long usuarioId) {
        super("El usuario con id " + usuarioId + " no existe en el servicio de Usuarios");
    }
}
