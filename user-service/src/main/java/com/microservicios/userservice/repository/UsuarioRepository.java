package com.microservicios.userservice.repository;

import com.microservicios.userservice.model.Usuario;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Almacen de usuarios en memoria.
 *
 * En un despliegue real cada microservicio tendria su propia base de datos
 * (patron "database per service"). Aqui se simula con un mapa concurrente
 * para mantener el ejercicio enfocado en la comunicacion entre servicios.
 */
@Repository
public class UsuarioRepository {

    private final Map<Long, Usuario> usuarios = new ConcurrentHashMap<>();
    private final AtomicLong secuenciaId = new AtomicLong(0);

    /** Carga datos de ejemplo para poder probar el sistema sin insertar nada. */
    @PostConstruct
    public void cargarDatosIniciales() {
        guardar(new Usuario(null, "Duvan Rodriguez", "duvan@ejemplo.com", "Medellin"));
        guardar(new Usuario(null, "Stiven Valencia", "stiven@ejemplo.com", "Bogota"));
        guardar(new Usuario(null, "Laura Gomez", "laura@ejemplo.com", "Cali"));
    }

    public List<Usuario> buscarTodos() {
        return new ArrayList<>(usuarios.values());
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return Optional.ofNullable(usuarios.get(id));
    }

    public Usuario guardar(Usuario usuario) {
        if (usuario.getId() == null) {
            usuario.setId(secuenciaId.incrementAndGet());
        }
        usuarios.put(usuario.getId(), usuario);
        return usuario;
    }

    public boolean eliminar(Long id) {
        return usuarios.remove(id) != null;
    }
}
