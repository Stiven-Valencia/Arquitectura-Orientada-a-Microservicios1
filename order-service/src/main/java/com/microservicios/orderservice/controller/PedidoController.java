package com.microservicios.orderservice.controller;

import com.microservicios.orderservice.client.UsuarioClient;
import com.microservicios.orderservice.dto.PedidoDetalleDTO;
import com.microservicios.orderservice.dto.UsuarioDTO;
import com.microservicios.orderservice.exception.PedidoNoEncontradoException;
import com.microservicios.orderservice.model.Pedido;
import com.microservicios.orderservice.repository.PedidoRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

/**
 * API REST del microservicio de Pedidos.
 *
 * Dos de sus endpoints demuestran la comunicacion entre microservicios:
 *  - POST /api/pedidos      valida contra el servicio de Usuarios antes de guardar
 *  - GET  /api/pedidos/{id} enriquece la respuesta con los datos del usuario
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private static final Logger log = LoggerFactory.getLogger(PedidoController.class);

    private final PedidoRepository repositorio;
    private final UsuarioClient usuarioClient;

    public PedidoController(PedidoRepository repositorio, UsuarioClient usuarioClient) {
        this.repositorio = repositorio;
        this.usuarioClient = usuarioClient;
    }

    /** GET /api/pedidos -> lista de pedidos sin enriquecer (no llama al otro servicio). */
    @GetMapping
    public List<Pedido> listar() {
        return repositorio.buscarTodos();
    }

    /**
     * POST /api/pedidos -> registra un pedido.
     *
     * Antes de guardar nada, pregunta al microservicio de Usuarios si el
     * usuarioId recibido existe. Si no existe, o si ese servicio no responde,
     * el pedido NO se crea: es una validacion de integridad referencial que
     * cruza la frontera entre servicios.
     */
    @PostMapping
    public ResponseEntity<PedidoDetalleDTO> crear(@Valid @RequestBody Pedido pedido) {
        log.info("Solicitud de creacion de pedido para el usuario {}", pedido.getUsuarioId());

        // 1. Comunicacion REST: se verifica el usuario contra el otro microservicio.
        UsuarioDTO usuario = usuarioClient.obtenerUsuario(pedido.getUsuarioId());

        // 2. Solo si el usuario existe se persiste el pedido localmente.
        pedido.setId(null); // el id lo asigna siempre este servicio
        pedido.setFechaCreacion(LocalDateTime.now());
        Pedido creado = repositorio.guardar(pedido);
        log.info("Pedido {} creado para el usuario {}", creado.getId(), usuario.nombre());

        // 3. Se devuelve la vista compuesta (pedido propio + usuario remoto).
        return ResponseEntity
                .created(URI.create("/api/pedidos/" + creado.getId()))
                .body(new PedidoDetalleDTO(creado, usuario));
    }

    /**
     * GET /api/pedidos/{id} -> pedido con los datos del usuario incluidos.
     *
     * El pedido solo guarda el usuarioId; el nombre y el correo se traen en
     * este momento desde el microservicio de Usuarios (composicion de API).
     */
    @GetMapping("/{id}")
    public PedidoDetalleDTO obtenerConUsuario(@PathVariable Long id) {
        Pedido pedido = repositorio.buscarPorId(id)
                .orElseThrow(() -> new PedidoNoEncontradoException(id));

        UsuarioDTO usuario = usuarioClient.obtenerUsuario(pedido.getUsuarioId());
        return new PedidoDetalleDTO(pedido, usuario);
    }

    /** GET /api/pedidos/usuario/{usuarioId} -> pedidos de un usuario, validando que exista. */
    @GetMapping("/usuario/{usuarioId}")
    public List<Pedido> listarPorUsuario(@PathVariable Long usuarioId) {
        usuarioClient.obtenerUsuario(usuarioId); // falla con 404 si el usuario no existe
        return repositorio.buscarPorUsuarioId(usuarioId);
    }
}
