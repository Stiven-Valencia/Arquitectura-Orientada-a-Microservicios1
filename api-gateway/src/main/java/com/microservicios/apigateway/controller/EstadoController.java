package com.microservicios.apigateway.controller;

import com.microservicios.apigateway.dto.EstadoServicio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Sonda de salud de los microservicios.
 *
 * La interfaz web consulta este endpoint cada pocos segundos para pintar cada
 * nodo del diagrama en verde o en rojo. Es una version minima de lo que en
 * produccion hariael service discovery (Eureka) o el health check de Kubernetes.
 */
@RestController
@RequestMapping("/api/sistema")
public class EstadoController {

    private final WebClient webClient;
    private final String urlUsuarios;
    private final String urlPedidos;

    public EstadoController(WebClient.Builder builder,
                            @Value("${servicios.usuarios.url}") String urlUsuarios,
                            @Value("${servicios.pedidos.url}") String urlPedidos) {
        this.webClient = builder.build();
        this.urlUsuarios = urlUsuarios;
        this.urlPedidos = urlPedidos;
    }

    /** GET /api/sistema/estado -> estado de los dos microservicios en paralelo. */
    @GetMapping("/estado")
    public Mono<Map<String, Object>> estado() {
        Mono<EstadoServicio> usuarios = sondear("Usuarios", urlUsuarios, "/api/usuarios");
        Mono<EstadoServicio> pedidos = sondear("Pedidos", urlPedidos, "/api/pedidos");

        // zip lanza ambas sondas a la vez: el estado tarda lo que la mas lenta,
        // no la suma de las dos.
        return Mono.zip(usuarios, pedidos)
                .map(tupla -> Map.of(
                        "gateway", new EstadoServicio("Gateway", "http://localhost:8080", true, 0),
                        "servicios", List.of(tupla.getT1(), tupla.getT2())
                ));
    }

    /**
     * Lanza una peticion de prueba a un servicio y mide cuanto tarda.
     * Cualquier fallo o timeout se traduce a "no disponible" en lugar de
     * propagarse: la sonda nunca debe tumbar al gateway.
     */
    private Mono<EstadoServicio> sondear(String nombre, String urlBase, String ruta) {
        long inicio = System.currentTimeMillis();
        return webClient.get()
                .uri(urlBase + ruta)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(2))
                .map(respuesta -> new EstadoServicio(
                        nombre, urlBase, true, System.currentTimeMillis() - inicio))
                .onErrorReturn(new EstadoServicio(
                        nombre, urlBase, false, System.currentTimeMillis() - inicio));
    }
}
