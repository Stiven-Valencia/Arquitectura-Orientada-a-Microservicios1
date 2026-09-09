package com.microservicios.userservice.exception;

/** Se lanza cuando se solicita un usuario que no existe en el almacen. */
public class UsuarioNoEncontradoException extends RuntimeException {

    public UsuarioNoEncontradoException(Long id) {
        super("No existe un usuario con id " + id);
    }
}
