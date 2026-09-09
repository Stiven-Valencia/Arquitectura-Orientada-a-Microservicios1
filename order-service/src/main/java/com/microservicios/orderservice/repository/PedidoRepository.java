package com.microservicios.orderservice.repository;

import com.microservicios.orderservice.model.Pedido;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Almacen de pedidos en memoria, independiente del almacen de usuarios.
 *
 * Esta separacion es intencional y es el punto central del ejercicio: cada
 * microservicio es dueno exclusivo de sus datos, por eso el servicio de
 * Pedidos no puede leer usuarios directamente y debe llamar por REST.
 */
@Repository
public class PedidoRepository {

    private final Map<Long, Pedido> pedidos = new ConcurrentHashMap<>();
    private final AtomicLong secuenciaId = new AtomicLong(0);

    public List<Pedido> buscarTodos() {
        return new ArrayList<>(pedidos.values());
    }

    public Optional<Pedido> buscarPorId(Long id) {
        return Optional.ofNullable(pedidos.get(id));
    }

    /** Pedidos de un usuario concreto, util para demostrar la relacion entre servicios. */
    public List<Pedido> buscarPorUsuarioId(Long usuarioId) {
        return pedidos.values().stream()
                .filter(pedido -> pedido.getUsuarioId().equals(usuarioId))
                .toList();
    }

    public Pedido guardar(Pedido pedido) {
        if (pedido.getId() == null) {
            pedido.setId(secuenciaId.incrementAndGet());
        }
        pedidos.put(pedido.getId(), pedido);
        return pedido;
    }
}
