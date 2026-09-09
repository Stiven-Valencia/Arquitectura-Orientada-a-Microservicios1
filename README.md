# Arquitectura Orientada a Microservicios

Implementación de dos microservicios independientes en **Java 21 + Spring Boot 3.4**
que se comunican entre sí mediante una **API REST**.

| Servicio | Rol | Puerto | Endpoint base |
|---|---|---|---|
| **api-gateway** | Punto de entrada único + interfaz web | `8080` | `/` y `/api/**` |
| **user-service** | Servicio de Usuarios — dueño de los datos de usuario | `8081` | `/api/usuarios` |
| **order-service** | Servicio de Pedidos — consume al de Usuarios | `8082` | `/api/pedidos` |

### 🖥️ Interfaz web

El sistema incluye un **panel de control visual** que se abre en
**<http://localhost:8080>** una vez levantados los tres servicios. Permite
gestionar usuarios y pedidos, y muestra **en vivo** cómo viajan las peticiones
entre los microservicios.

### 📚 Documentación

| Documento | Contenido |
|---|---|
| **[docs/informe.md](docs/informe.md)** | Informe técnico completo: marco conceptual, implementación, pruebas y conclusiones |

---

## Arquitectura en una imagen

```
                    ┌──────────────────────────┐
                    │        NAVEGADOR         │
                    └────────────┬─────────────┘
                                 │ :8080
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

El servicio de Pedidos **no guarda** los datos del usuario: solo el `usuarioId`.
Cuando los necesita, los pide por HTTP al servicio de Usuarios.

---

## Requisitos

- **JDK 21** o superior
- **Maven 3.9+**

```bash
java -version   # openjdk 21
mvn -version    # Apache Maven 3.9.x
```

---

## Cómo ejecutar

### 1. Compilar los tres servicios

```bash
cd user-service  && mvn clean package -DskipTests && cd ..
cd order-service && mvn clean package -DskipTests && cd ..
cd api-gateway   && mvn clean package -DskipTests && cd ..
```

### 2. Levantar los servicios en **tres terminales separadas**

**Terminal 1 — Servicio de Usuarios (arrancar primero):**
```bash
java -jar user-service/target/user-service-1.0.0.jar
```

**Terminal 2 — Servicio de Pedidos:**
```bash
java -jar order-service/target/order-service-1.0.0.jar
```

**Terminal 3 — API Gateway:**
```bash
java -jar api-gateway/target/api-gateway-1.0.0.jar
```

> El servicio de Usuarios conviene arrancarlo primero, ya que el de Pedidos depende de él.

### 3. Abrir la interfaz

```
http://localhost:8080
```

El servicio de Usuarios carga **3 usuarios de ejemplo** al iniciar (ids `1`, `2` y `3`),
así se puede probar el sistema sin insertar nada.

---

## La interfaz web

El panel en `http://localhost:8080` está pensado para **hacer visible** la
comunicación entre microservicios, que normalmente ocurre sin que nadie la vea.

| Zona | Qué hace |
|---|---|
| **Diagrama en vivo** | Cuatro nodos conectados. En cada petición una partícula recorre la ruta que se está usando. La conexión **Pedidos → Usuarios** se ilumina en violeta cuando ocurre el salto entre servicios. |
| **Semáforo de salud** | Cada servicio tiene un punto verde/rojo que se actualiza solo cada 5 segundos. Si apagas un servicio, lo ves en pantalla. |
| **Servicio de Usuarios** | Crear, listar y eliminar usuarios. |
| **Servicio de Pedidos** | Crear pedidos eligiendo un usuario; *Ver usuario* fuerza la llamada REST entre servicios. |
| **Botón de escenario 404** | Envía un pedido con `usuarioId: 999` para demostrar que la validación cruza la frontera entre servicios. |
| **Consola de peticiones** | Método, ruta, código HTTP y latencia de cada llamada. Las líneas violetas indentadas marcan el salto interno Pedidos → Usuarios, que el navegador nunca ve. |

### Demostración sugerida para la sustentación

1. Abre `http://localhost:8080` — los dos nodos aparecen en verde.
2. Crea un pedido para un usuario existente → observa la partícula viajar y la
   línea violeta en la consola: **ahí está la comunicación entre servicios**.
3. Pulsa *Ver usuario* en ese pedido → el nombre y correo no estaban guardados
   en Pedidos, se acaban de traer del servicio de Usuarios.
4. Pulsa el botón de **usuario 999** → responde `404` y el pedido no se crea.
5. **Apaga el servicio de Usuarios** (Ctrl+C en la terminal 1) → en 5 segundos
   su nodo se pone rojo solo. Intenta crear un pedido → responde `503`, no se
   queda colgado.

---

## Endpoints

> Todos los endpoints son accesibles **también a través del gateway** en el
> puerto `8080` con la misma ruta. La interfaz web siempre usa el gateway;
> los puertos directos sirven para probar cada servicio de forma aislada.

### Servicio de Usuarios — `http://localhost:8081` (o `:8080` vía gateway)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/usuarios` | Lista todos los usuarios |
| `GET` | `/api/usuarios/{id}` | Obtiene un usuario — **lo consume el servicio de Pedidos** |
| `POST` | `/api/usuarios` | Crea un usuario |
| `DELETE` | `/api/usuarios/{id}` | Elimina un usuario |

### Servicio de Pedidos — `http://localhost:8082` (o `:8080` vía gateway)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/pedidos` | Lista los pedidos (sin llamar al otro servicio) |
| `POST` | `/api/pedidos` | Crea un pedido — **valida el usuario vía REST** |
| `GET` | `/api/pedidos/{id}` | Pedido + datos del usuario — **compone vía REST** |
| `GET` | `/api/pedidos/usuario/{usuarioId}` | Pedidos de un usuario, validando que exista |

### API Gateway — `http://localhost:8080`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/` | Interfaz web del panel de control |
| `GET` | `/api/sistema/estado` | Salud de ambos microservicios, con latencias |
| `*` | `/api/usuarios/**` | Reenvía al servicio de Usuarios |
| `*` | `/api/pedidos/**` | Reenvía al servicio de Pedidos |

---

## Pruebas de la comunicación entre servicios

Las siguientes salidas son **reales**, capturadas con ambos servicios en ejecución.

### ✅ Caso 1 — Crear un pedido para un usuario que existe

```bash
curl -X POST http://localhost:8082/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{"usuarioId":2,"producto":"Teclado mecanico","cantidad":1,"total":250000}'
```

**`201 Created`** — el servicio de Pedidos consultó al de Usuarios, confirmó que
existe y devolvió el pedido junto con los datos del usuario:

```json
{
  "pedido": {
    "id": 1,
    "usuarioId": 2,
    "producto": "Teclado mecanico",
    "cantidad": 1,
    "total": 250000.0,
    "fechaCreacion": "2026-09-07T09:04:41.000"
  },
  "usuario": {
    "id": 2,
    "nombre": "Stiven Valencia",
    "correo": "stiven@ejemplo.com",
    "ciudad": "Bogota"
  }
}
```

### ✅ Caso 2 — Consultar el pedido con los datos del usuario

```bash
curl http://localhost:8082/api/pedidos/1
```

**`200 OK`** — devuelve la misma estructura compuesta. El bloque `usuario` **no**
está almacenado en el servicio de Pedidos: se trae por REST en cada consulta.

### ❌ Caso 3 — Usuario inexistente

```bash
curl -X POST http://localhost:8082/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{"usuarioId":999,"producto":"Monitor","cantidad":1,"total":800000}'
```

**`404 Not Found`** — el pedido **no se creó**, porque el servicio de Usuarios
respondió que ese usuario no existe:

```json
{
  "estado": 404,
  "error": "Not Found",
  "mensaje": "El usuario con id 999 no existe en el servicio de Usuarios",
  "servicio": "order-service"
}
```

### ⚠️ Caso 4 — Servicio de Usuarios caído (tolerancia a fallos)

Deteniendo el servicio de Usuarios y volviendo a pedir:

```bash
curl -X POST http://localhost:8082/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{"usuarioId":1,"producto":"Mouse","cantidad":2,"total":90000}'
```

**`503 Service Unavailable`** — el servicio de Pedidos **no se cuelga**: el timeout
de 3 s corta la espera y responde con un error claro y distinto del 404.

```json
{
  "estado": 503,
  "error": "Service Unavailable",
  "mensaje": "El servicio de Usuarios no esta disponible: no responde en http://localhost:8081/api/usuarios/1",
  "servicio": "order-service"
}
```

> Distinguir **404** (el dato no existe) de **503** (el sistema falló) es
> fundamental en sistemas distribuidos: el primero es culpa del cliente, el
> segundo es una falla de infraestructura que el cliente puede reintentar.

---

## Estructura del proyecto

```
Arquitectura-Orientada-a-Microservicios/
├── README.md
├── docs/
│   └── informe.md                   ← informe técnico
│
├── api-gateway/                     ← API GATEWAY + INTERFAZ WEB (8080)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/microservicios/apigateway/
│       │   ├── ApiGatewayApplication.java
│       │   ├── config/FiltroRegistroPeticiones.java
│       │   ├── controller/EstadoController.java
│       │   └── dto/EstadoServicio.java
│       └── resources/
│           ├── application.yml       ← reglas de enrutamiento
│           └── static/               ← LA INTERFAZ WEB
│               ├── index.html
│               ├── css/estilos.css
│               └── js/app.js         ← diagrama animado + consola
│
├── user-service/                    ← MICROSERVICIO DE USUARIOS (8081)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/microservicios/userservice/
│       │   ├── UserServiceApplication.java
│       │   ├── model/Usuario.java
│       │   ├── repository/UsuarioRepository.java
│       │   ├── controller/UsuarioController.java
│       │   └── exception/
│       └── resources/application.properties
│
└── order-service/                   ← MICROSERVICIO DE PEDIDOS (8082)
    ├── pom.xml
    └── src/main/
        ├── java/com/microservicios/orderservice/
        │   ├── OrderServiceApplication.java
        │   ├── model/Pedido.java
        │   ├── dto/            UsuarioDTO · PedidoDetalleDTO
        │   ├── client/         UsuarioClient  ★ comunicación REST
        │   ├── config/         RestTemplateConfig (timeouts)
        │   ├── repository/PedidoRepository.java
        │   ├── controller/PedidoController.java
        │   └── exception/
        └── resources/application.properties
```

**El archivo clave** es
[`UsuarioClient.java`](order-service/src/main/java/com/microservicios/orderservice/client/UsuarioClient.java):
es la única clase de todo el sistema que conoce la existencia del otro
microservicio y donde ocurre la llamada REST.

---

## Configuración

La URL del servicio remoto no está fija en el código. Se puede cambiar sin
recompilar, mediante variable de entorno:

```bash
USUARIOS_SERVICE_URL=http://otro-host:8081 java -jar order-service/target/order-service-1.0.0.jar
```

| Propiedad | Valor por defecto | Descripción |
|---|---|---|
| `usuarios.service.url` | `http://localhost:8081` | URL base del servicio de Usuarios |
| `usuarios.service.timeout-conexion-ms` | `3000` | Timeout de conexión |
| `usuarios.service.timeout-lectura-ms` | `3000` | Timeout de lectura |
