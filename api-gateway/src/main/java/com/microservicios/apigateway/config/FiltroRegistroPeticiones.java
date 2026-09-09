package com.microservicios.apigateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Registra en consola cada peticion que atraviesa el gateway.
 *
 * Ilustra por que existe un API Gateway: los aspectos transversales
 * (logging, autenticacion, limitacion de trafico) se resuelven una sola vez
 * aqui, en lugar de repetirse dentro de cada microservicio.
 */
@Component
public class FiltroRegistroPeticiones implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(FiltroRegistroPeticiones.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long inicio = System.currentTimeMillis();
        String metodo = exchange.getRequest().getMethod().name();
        String ruta = exchange.getRequest().getURI().getPath();

        return chain.filter(exchange).doFinally(senal -> {
            long duracion = System.currentTimeMillis() - inicio;
            Integer codigo = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value()
                    : null;
            log.info("[GATEWAY] {} {} -> {} ({} ms)", metodo, ruta, codigo, duracion);
        });
    }

    /** Se ejecuta antes que el resto de filtros para medir el tiempo total. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
