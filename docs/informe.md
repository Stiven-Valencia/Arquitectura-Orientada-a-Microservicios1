# Implementación de una Arquitectura Orientada a Microservicios

**Autor:** Duvan Rodríguez
**Repositorio:** https://github.com/Stiven-Valencia/Arquitectura-Orientada-a-Microservicios
**Tecnologías:** Java 21 · Spring Boot 3.4.1 · Spring Cloud Gateway 2024.0.0 · Maven 3.9

---

## 1. Introducción

Este documento describe la implementación de un sistema distribuido compuesto por
microservicios independientes que se comunican mediante una API REST. El objetivo
del ejercicio era construir dos servicios sencillos, simular la comunicación
entre ellos y documentar el diseño; el alcance se amplió posteriormente con un
API Gateway y una interfaz web que permite observar esa comunicación en tiempo
real.

La arquitectura de microservicios propone descomponer una aplicación en servicios
pequeños, autónomos y desplegables por separado, cada uno responsable de una
capacidad de negocio concreta. Frente al modelo monolítico, esto introduce
complejidad operativa a cambio de independencia de despliegue, escalabilidad
selectiva y aislamiento de fallos.

---

## 2. Objetivos

### 2.1 Objetivo general

Implementar y documentar un sistema basado en microservicios que demuestre de
forma práctica los principios de autonomía de datos, comunicación por contrato y
tolerancia a fallos.

### 2.2 Objetivos específicos

1. Construir dos microservicios independientes con responsabilidades separadas.
2. Establecer comunicación entre ellos mediante una API REST.
3. Documentar el diseño mediante diagramas de arquitectura.
4. Implementar un punto de entrada único mediante un API Gateway.
5. Desarrollar una interfaz que haga visible la comunicación entre servicios.

---

## 3. Marco conceptual

### 3.1 Del monolito a los microservicios

En una aplicación monolítica, usuarios y pedidos residirían en la misma base de
datos. Obtener el nombre del dueño de un pedido sería una operación trivial: un
`JOIN` entre dos tablas dentro de la misma transacción.

En una arquitectura de microservicios esa operación **no está disponible**, y la
restricción es deliberada. Cada servicio es dueño exclusivo de sus datos, de modo
que el servicio de Pedidos no puede consultar la tabla de usuarios: debe
solicitar esa información al servicio de Usuarios a través de la red.

Esta restricción tiene un costo evidente —una llamada de red es órdenes de
magnitud más lenta y más frágil que un `JOIN`— pero habilita tres propiedades:

| Propiedad | Implicación práctica |
|---|---|
| **Despliegue independiente** | Actualizar el servicio de Pedidos no obliga a recompilar ni redesplegar el de Usuarios. |
| **Escalabilidad selectiva** | Si los pedidos reciben diez veces más tráfico, se escala solo ese servicio. |
| **Aislamiento de fallos** | Un error en Pedidos no detiene la consulta de usuarios. |

### 3.2 Principios aplicados

**Responsabilidad única.** Cada servicio resuelve un solo dominio de negocio.

**Autonomía de datos** (*database per service*). Cada servicio gestiona su propio
almacenamiento. Ningún servicio accede al almacén de otro.

**Comunicación por contrato.** Los servicios se acoplan únicamente al formato de
los mensajes intercambiados —URL y estructura JSON—, nunca a clases compartidas
en tiempo de compilación.

**Tolerancia a fallos.** El sistema asume que la red falla y que los servicios
remotos pueden estar caídos, y define un comportamiento explícito para esos casos.

---

## 4. Arquitectura del sistema

### 4.1 Componentes

| Componente | Responsabilidad | Puerto |
|---|---|---|
| **api-gateway** | Punto de entrada único; enruta las peticiones y sirve la interfaz web | 8080 |
| **user-service** | Gestión de usuarios; fuente de verdad de esos datos | 8081 |
| **order-service** | Gestión de pedidos; consume el servicio de Usuarios | 8082 |

### 4.2 Diagrama de componentes

```
                    ┌──────────────────────────┐
                    │        NAVEGADOR         │
                    │   Panel web + diagrama   │
                    └────────────┬─────────────┘
                                 │ HTTP :8080
                    ┌────────────▼─────────────┐
                    │      API GATEWAY         │
                    │  sirve la UI + enruta    │
                    └───────┬──────────┬───────┘
                            │          │
              /api/usuarios │          │ /api/pedidos
                            ▼          ▼
   ┌──────────────────────────┐   ┌──────────────────────────┐
   │  USUARIOS      :8081     │◀──┤  PEDIDOS       :8082     │
   │  UsuarioController       │   │  PedidoController        │
   │  UsuarioRepository       │   │  PedidoRepository        │
   └──────────────────────────┘   │  UsuarioClient ──────────┘
              ▲                   └──────────────────────────┘
              └──── GET /api/usuarios/{id} ── COMUNICACIÓN REST
```

### 4.3 Justificación del API Gateway

Sin gateway, el navegador debería invocar directamente los puertos 8081 y 8082,
lo que introduce dos problemas:

1. **CORS.** El navegador bloquea peticiones dirigidas a un origen distinto del
   que sirvió la página. Resolverlo exigiría habilitar CORS en cada
   microservicio, relajando su configuración de seguridad.
2. **Acoplamiento del cliente.** El frontend tendría que conocer la dirección y
   el puerto de cada servicio; cualquier cambio en la topología obligaría a
   modificarlo.

El gateway resuelve ambos: la interfaz se sirve desde el mismo origen al que
dirige sus peticiones, y el cliente conoce una sola dirección.

Adicionalmente, el gateway concentra los **aspectos transversales**. El filtro
`FiltroRegistroPeticiones` registra método, ruta, código de respuesta y latencia
de toda petición que atraviesa el sistema. Implementarlo una sola vez en el
gateway evita duplicar esa lógica en cada microservicio; el mismo punto serviría
para autenticación o limitación de tráfico.

---

## 5. Implementación

### 5.1 Servicio de Usuarios

Expone la API REST que constituye la fuente de verdad de los datos de usuario. No
depende de ningún otro servicio del sistema.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/usuarios` | Lista todos los usuarios |
| `GET` | `/api/usuarios/{id}` | Obtiene un usuario — **consumido por el servicio de Pedidos** |
| `POST` | `/api/usuarios` | Registra un usuario |
| `DELETE` | `/api/usuarios/{id}` | Elimina un usuario |

El almacenamiento se implementó con un `ConcurrentHashMap` en memoria, precargado
con tres usuarios de ejemplo. En un entorno productivo correspondería una base de
datos PostgreSQL gestionada mediante Spring Data JPA.

### 5.2 Servicio de Pedidos

Gestiona los pedidos y **consume** el servicio de Usuarios. Una decisión de
diseño central es que la entidad `Pedido` almacena únicamente el `usuarioId`, sin
copia alguna del nombre o el correo:

```java
public class Pedido {
    private Long id;
    private Long usuarioId;      // referencia, no copia
    private String producto;
    private Integer cantidad;
    private Double total;
    private LocalDateTime fechaCreacion;
}
```

Duplicar los datos del usuario generaría inconsistencia: si el usuario
modificara su correo, la copia almacenada en Pedidos quedaría desactualizada sin
que ningún mecanismo lo detectara. Los datos se solicitan bajo demanda, patrón
conocido como **composición de API**.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/pedidos` | Lista los pedidos (sin llamada remota) |
| `POST` | `/api/pedidos` | Crea un pedido — **valida el usuario vía REST** |
| `GET` | `/api/pedidos/{id}` | Pedido con datos del usuario — **compone vía REST** |
| `GET` | `/api/pedidos/usuario/{usuarioId}` | Pedidos de un usuario, validando que exista |

### 5.3 La comunicación entre servicios

Toda la comunicación remota se concentra en una única clase, `UsuarioClient`. Es
la única del servicio de Pedidos que conoce la existencia del servicio de
Usuarios; el resto del código depende de esta abstracción, no de HTTP. Si la
comunicación migrara a gRPC o a mensajería asíncrona, solo cambiaría esta clase.

Su aspecto más relevante es la **traducción diferenciada de errores**:

```java
try {
    UsuarioDTO usuario = restTemplate.getForObject(url, UsuarioDTO.class);
    return usuario;

} catch (HttpClientErrorException.NotFound ex) {
    // El servicio respondió 404: el dato no existe.
    throw new UsuarioInexistenteException(usuarioId);

} catch (ResourceAccessException ex) {
    // Timeout o servicio apagado: el sistema falló.
    throw new ServicioUsuariosNoDisponibleException("no responde en " + url, ex);

} catch (RestClientException ex) {
    // Cualquier otro fallo HTTP (5xx, respuesta ilegible).
    throw new ServicioUsuariosNoDisponibleException(ex.getMessage(), ex);
}
```

La distinción entre ambos casos es determinante:

| Situación | Excepción | Respuesta HTTP | Significado para el cliente |
|---|---|---|---|
| El usuario no existe | `UsuarioInexistenteException` | `404` | Error en la petición; corregir los datos |
| El servicio no responde | `ServicioUsuariosNoDisponibleException` | `503` | Falla de infraestructura; reintentar más tarde |

Tratar ambos casos de forma idéntica es un error frecuente en la integración de
microservicios: un `404` indica que el cliente debe corregir su solicitud,
mientras que un `503` indica que la solicitud era correcta y conviene reintentarla.

### 5.4 Prevención de fallos en cascada

El cliente HTTP se configura con timeouts explícitos:

```java
return builder
        .connectTimeout(Duration.ofMillis(3000))
        .readTimeout(Duration.ofMillis(3000))
        .build();
```

Sin este límite, la caída del servicio de Usuarios dejaría hilos del servicio de
Pedidos bloqueados esperando indefinidamente. Al agotarse el pool de hilos, el
servicio de Pedidos dejaría de responder también, propagando la falla: el
fenómeno conocido como **fallo en cascada**. El timeout acota la espera y permite
degradar el servicio de forma controlada.

### 5.5 Desacoplamiento de configuración

La dirección del servicio remoto no está fijada en el código:

```properties
usuarios.service.url=${USUARIOS_SERVICE_URL:http://localhost:8081}
```

El mismo artefacto compilado funciona en desarrollo local, en contenedores Docker
o en la nube, cambiando únicamente una variable de entorno.

### 5.6 El DTO como frontera

El servicio de Pedidos no reutiliza la clase `Usuario` del otro servicio; define
su propia vista del dato:

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsuarioDTO(Long id, String nombre, String correo, String ciudad) { }
```

Compartir clases entre microservicios los acoplaría en tiempo de compilación,
anulando la independencia de despliegue. La anotación
`@JsonIgnoreProperties(ignoreUnknown = true)` aporta compatibilidad hacia atrás:
el servicio de Usuarios puede incorporar campos nuevos a su respuesta sin romper
al consumidor.

---

## 6. La interfaz web

La interfaz, servida por el gateway en `http://localhost:8080`, se diseñó con un
propósito específico: **hacer visible** la comunicación entre servicios, que por
naturaleza ocurre entre servidores y resulta invisible para el usuario final.

| Elemento | Función |
|---|---|
| **Diagrama en vivo** | Representa los cuatro nodos del sistema. En cada petición, una partícula recorre la ruta utilizada. La conexión Pedidos → Usuarios se resalta cuando ocurre el salto entre servicios. |
| **Indicadores de salud** | Cada servicio muestra un estado verde o rojo, actualizado cada 5 segundos mediante `/api/sistema/estado`. |
| **Formularios de gestión** | Operaciones de creación, consulta y eliminación sobre ambos servicios. |
| **Escenario de error** | Un control dispara un pedido con `usuarioId: 999` para demostrar la validación entre servicios. |
| **Consola de peticiones** | Registra método, ruta, código HTTP y latencia. Las entradas destacadas señalan el salto interno Pedidos → Usuarios. |

El elemento más significativo es esa última distinción: el navegador ejecuta
**una** petición, pero el sistema realiza **dos** saltos de red. La consola hace
explícita esa diferencia entre lo que percibe el usuario y lo que ocurre
realmente en la infraestructura.

---

## 7. Pruebas y resultados

Se ejecutaron cuatro escenarios con los tres servicios en funcionamiento. Las
salidas corresponden a ejecuciones reales del sistema.

### 7.1 Caso 1 — Creación con usuario existente

**Petición:**
```bash
curl -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{"usuarioId":3,"producto":"Monitor 27 pulgadas","cantidad":1,"total":1200000}'
```

**Respuesta — `201 Created`:**
```json
{
  "pedido": {
    "id": 1,
    "usuarioId": 3,
    "producto": "Monitor 27 pulgadas",
    "cantidad": 1,
    "total": 1200000.0,
    "fechaCreacion": "2026-09-07T13:15:40.841"
  },
  "usuario": {
    "id": 3,
    "nombre": "Laura Gomez",
    "correo": "laura@ejemplo.com",
    "ciudad": "Cali"
  }
}
```

El bloque `usuario` no procede del almacén de Pedidos: se obtuvo del servicio de
Usuarios durante el procesamiento de esta misma petición.

### 7.2 Caso 2 — Consulta con composición de datos

`GET /api/pedidos/1` devuelve `200 OK` con idéntica estructura compuesta,
confirmando que los datos del usuario se recuperan en cada consulta y no se
almacenan localmente.

### 7.3 Caso 3 — Usuario inexistente

**Petición:** `POST /api/pedidos` con `usuarioId: 999`.

**Respuesta — `404 Not Found`:**
```json
{
  "estado": 404,
  "error": "Not Found",
  "mensaje": "El usuario con id 999 no existe en el servicio de Usuarios",
  "servicio": "order-service"
}
```

El pedido no se creó. Se verificó la integridad referencial a través de la
frontera entre dos servicios con almacenes independientes.

### 7.4 Caso 4 — Servicio remoto no disponible

Con el servicio de Usuarios detenido:

**Respuesta — `503 Service Unavailable`:**
```json
{
  "estado": 503,
  "error": "Service Unavailable",
  "mensaje": "El servicio de Usuarios no esta disponible: no responde en http://localhost:8081/api/usuarios/1",
  "servicio": "order-service"
}
```

El servicio de Pedidos respondió en aproximadamente tres segundos —el timeout
configurado— en lugar de permanecer bloqueado. El indicador de salud de la
interfaz reflejó automáticamente la caída en el siguiente ciclo de sondeo.

### 7.5 Registro del gateway

El filtro global registró cada petición atravesando el sistema:

```
[GATEWAY] GET  /api/usuarios -> 200 (386 ms)
[GATEWAY] POST /api/pedidos  -> 201 (292 ms)
[GATEWAY] POST /api/pedidos  -> 404 (47 ms)
```

### 7.6 Resumen

| # | Escenario | Esperado | Obtenido | Resultado |
|---|---|---|---|---|
| 1 | Pedido con usuario válido | `201` + datos compuestos | `201` | ✔ |
| 2 | Consulta de pedido | `200` + datos del usuario | `200` | ✔ |
| 3 | Usuario inexistente | `404`, sin crear el pedido | `404` | ✔ |
| 4 | Servicio caído | `503` en ~3 s | `503` | ✔ |

---

## 8. Limitaciones y trabajo futuro

Esta implementación tiene alcance didáctico. Un despliegue productivo requeriría:

**Persistencia real.** Los datos residen en memoria y se pierden al reiniciar
cada servicio. Correspondería una base de datos PostgreSQL independiente por
servicio, gestionada con Spring Data JPA.

**Service Discovery.** Las direcciones de los servicios están configuradas
manualmente. Herramientas como Eureka o Consul permiten que los servicios se
registren y se localicen dinámicamente, requisito para escalar horizontalmente.

**Circuit Breaker.** Los timeouts evitan el bloqueo indefinido, pero el sistema
sigue intentando contactar un servicio que ya se sabe caído. Resilience4j
interrumpiría las llamadas tras detectar fallos consecutivos y ofrecería
respuestas de respaldo.

**Comunicación asíncrona.** Las operaciones que no requieren respuesta inmediata
podrían resolverse mediante eventos sobre RabbitMQ o Kafka, eliminando el
acoplamiento temporal entre servicios.

**Trazabilidad distribuida.** Con más servicios, seguir una petición a través del
sistema se vuelve complejo. Micrometer Tracing junto a Zipkin permite visualizar
la traza completa.

**Seguridad.** El sistema carece de autenticación. El gateway sería el punto
natural para validar tokens JWT antes de enrutar.

---

## 9. Conclusiones

Se implementó un sistema distribuido funcional compuesto por dos microservicios
independientes y un API Gateway, verificando en ejecución los cuatro escenarios
planteados.

El ejercicio evidencia que la comunicación entre microservicios no se reduce a
realizar una llamada HTTP. Las decisiones que determinan la robustez del sistema
son las que gestionan lo que ocurre cuando esa llamada **no** funciona como se
espera: la distinción entre un dato ausente y un servicio caído, los timeouts que
previenen fallos en cascada, y la configuración externalizada que permite
reubicar los servicios sin recompilar.

La restricción central de la arquitectura —que un servicio no pueda leer los
datos de otro— es a la vez su principal costo y su principal beneficio. Impone
una llamada de red donde un monolito resolvería con un `JOIN`, pero es
precisamente esa frontera la que permite desplegar, escalar y hacer fallar cada
servicio de forma independiente.

---

## 10. Referencias

- Newman, S. (2021). *Building Microservices: Designing Fine-Grained Systems* (2ª ed.). O'Reilly Media.
- Richardson, C. (2018). *Microservices Patterns: With Examples in Java*. Manning Publications.
- Documentación oficial de Spring Boot. https://docs.spring.io/spring-boot/
- Documentación oficial de Spring Cloud Gateway. https://docs.spring.io/spring-cloud-gateway/
- Fowler, M. & Lewis, J. *Microservices: a definition of this new architectural term*. https://martinfowler.com/articles/microservices.html
