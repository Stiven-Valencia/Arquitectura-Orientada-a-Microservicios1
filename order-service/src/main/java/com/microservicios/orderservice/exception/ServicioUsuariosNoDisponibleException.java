package com.microservicios.orderservice.exception;

/**
 * El microservicio de Usuarios esta caido, no responde o devolvio un error 5xx.
 *
 * Se distingue a proposito de UsuarioInexistenteException: aqui el sistema
 * fallo (503), alla el dato simplemente no existia (404). Confundir ambos casos
 * es un error clasico al integrar microservicios.
 */
public class ServicioUsuariosNoDisponibleException extends RuntimeException {

    public ServicioUsuariosNoDisponibleException(String detalle, Throwable causa) {
        super("El servicio de Usuarios no esta disponible: " + detalle, causa);
    }
}
