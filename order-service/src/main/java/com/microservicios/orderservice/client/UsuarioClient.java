package com.microservicios.orderservice.client;

import com.microservicios.orderservice.dto.UsuarioDTO;
import com.microservicios.orderservice.exception.ServicioUsuariosNoDisponibleException;
import com.microservicios.orderservice.exception.UsuarioInexistenteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente HTTP hacia el microservicio de Usuarios.
 *
 * Es la unica clase de este servicio que conoce la existencia del otro:
 * el resto del codigo depende de esta abstraccion, no de HTTP. Si manana
 * la comunicacion pasara a gRPC o a mensajeria asincrona, solo cambiaria
 * esta clase.
 */
@Component
public class UsuarioClient {

    private static final Logger log = LoggerFactory.getLogger(UsuarioClient.class);

    private final RestTemplate restTemplate;
    private final String urlBaseUsuarios;

    public UsuarioClient(RestTemplate restTemplate,
                         @Value("${usuarios.service.url}") String urlBaseUsuarios) {
        this.restTemplate = restTemplate;
        this.urlBaseUsuarios = urlBaseUsuarios;
    }

    /**
     * Consulta GET /api/usuarios/{id} en el microservicio de Usuarios.
     *
     * Traduce cada resultado de red a una excepcion de dominio distinta:
     *  - 404          -> UsuarioInexistenteException (el dato no existe)
     *  - 5xx / timeout -> ServicioUsuariosNoDisponibleException (el sistema fallo)
     *
     * @param usuarioId identificador del usuario a consultar
     * @return los datos del usuario tal como los publica el otro servicio
     */
    public UsuarioDTO obtenerUsuario(Long usuarioId) {
        String url = urlBaseUsuarios + "/api/usuarios/" + usuarioId;
        log.info("Llamada REST saliente -> GET {}", url);

        try {
            UsuarioDTO usuario = restTemplate.getForObject(url, UsuarioDTO.class);
            if (usuario == null) {
                throw new ServicioUsuariosNoDisponibleException(
                        "respuesta vacia al consultar " + url, null);
            }
            log.info("Respuesta recibida del servicio de Usuarios: {}", usuario.nombre());
            return usuario;

        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("El servicio de Usuarios respondio 404 para el id {}", usuarioId);
            throw new UsuarioInexistenteException(usuarioId);

        } catch (ResourceAccessException ex) {
            // Cubre timeout de conexion/lectura y servicio apagado.
            log.error("No se pudo contactar al servicio de Usuarios en {}", url, ex);
            throw new ServicioUsuariosNoDisponibleException("no responde en " + url, ex);

        } catch (RestClientException ex) {
            // Cualquier otro fallo HTTP (5xx, respuesta ilegible, etc.).
            log.error("Fallo la llamada al servicio de Usuarios en {}", url, ex);
            throw new ServicioUsuariosNoDisponibleException(ex.getMessage(), ex);
        }
    }
}
