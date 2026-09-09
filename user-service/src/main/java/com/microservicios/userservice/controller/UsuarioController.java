package com.microservicios.userservice.controller;

import com.microservicios.userservice.exception.UsuarioNoEncontradoException;
import com.microservicios.userservice.model.Usuario;
import com.microservicios.userservice.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * API REST publica del microservicio de Usuarios.
 *
 * Es el unico punto de acceso a los datos de usuario: cualquier otro
 * microservicio que los necesite debe consumir estos endpoints en lugar de
 * leer directamente el almacen (principio de autonomia de datos).
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private static final Logger log = LoggerFactory.getLogger(UsuarioController.class);

    private final UsuarioRepository repositorio;

    public UsuarioController(UsuarioRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** GET /api/usuarios -> lista completa de usuarios. */
    @GetMapping
    public List<Usuario> listar() {
        List<Usuario> encontrados = repositorio.buscarTodos();
        log.info("Se solicito la lista de usuarios. Total devuelto: {}", encontrados.size());
        return encontrados;
    }

    /**
     * GET /api/usuarios/{id} -> un usuario concreto.
     *
     * Este es el endpoint que consume el microservicio de Pedidos para
     * validar que el usuario existe antes de registrar un pedido.
     */
    @GetMapping("/{id}")
    public Usuario obtenerPorId(@PathVariable Long id) {
        log.info("Consulta del usuario con id {}", id);
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));
    }

    /** POST /api/usuarios -> registra un usuario nuevo. */
    @PostMapping
    public ResponseEntity<Usuario> crear(@Valid @RequestBody Usuario usuario) {
        usuario.setId(null); // el id siempre lo asigna el servicio, nunca el cliente
        Usuario creado = repositorio.guardar(usuario);
        log.info("Usuario creado con id {}", creado.getId());
        return ResponseEntity
                .created(URI.create("/api/usuarios/" + creado.getId()))
                .body(creado);
    }

    /** DELETE /api/usuarios/{id} -> elimina un usuario. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!repositorio.eliminar(id)) {
            throw new UsuarioNoEncontradoException(id);
        }
        log.info("Usuario con id {} eliminado", id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
