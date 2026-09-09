/* =========================================================
   Panel de Arquitectura de Microservicios

   Toda petición sale hacia el mismo origen (el API Gateway, puerto 8080).
   El navegador nunca habla con los puertos 8081 ni 8082 directamente: por eso
   no hay problema de CORS y el cliente solo necesita conocer una dirección.
   ========================================================= */

(() => {
    "use strict";

    // ---------------------------------------------------------------
    // Referencias al DOM
    // ---------------------------------------------------------------
    const $ = (id) => document.getElementById(id);

    const dom = {
        estadoGlobal:      $("estadoGlobal"),
        estadoGlobalTexto: $("estadoGlobalTexto"),
        etiquetaFlujo:     $("etiquetaFlujo"),
        particula:         $("particula"),
        listaUsuarios:     $("listaUsuarios"),
        listaPedidos:      $("listaPedidos"),
        selectorUsuario:   $("pedidoUsuarioId"),
        consola:           $("consola"),
        aviso:             $("aviso"),
        formUsuario:       $("formUsuario"),
        formPedido:        $("formPedido"),
    };

    const vias = {
        navegadorGateway: $("viaNavegadorGateway"),
        gatewayUsuarios:  $("viaGatewayUsuarios"),
        gatewayPedidos:   $("viaGatewayPedidos"),
        pedidosUsuarios:  $("viaPedidosUsuarios"),
    };

    const nodos = {
        navegador: $("nodoNavegador"),
        gateway:   $("nodoGateway"),
        usuarios:  $("nodoUsuarios"),
        pedidos:   $("nodoPedidos"),
    };

    const salud = {
        usuarios: $("saludUsuarios"),
        pedidos:  $("saludPedidos"),
    };

    // ---------------------------------------------------------------
    // Capa de red: un solo lugar por donde salen todas las peticiones
    // ---------------------------------------------------------------

    /**
     * Ejecuta una petición contra el gateway, la cronometra, la registra en la
     * consola y anima el diagrama según el servicio de destino.
     *
     * @param {string} metodo  verbo HTTP
     * @param {string} ruta    ruta relativa, siempre bajo /api/**
     * @param {object} [cuerpo] payload JSON opcional
     * @returns {Promise<{ok: boolean, codigo: number, datos: any}>}
     */
    async function pedir(metodo, ruta, cuerpo) {
        const destino = ruta.includes("/pedidos") ? "pedidos" : "usuarios";

        // Un pedido consultado o creado obliga al servicio de Pedidos a llamar
        // al de Usuarios: ese salto interno se dibuja aparte.
        const haySaltoInterno =
            destino === "pedidos" &&
            (metodo === "POST" || /\/api\/pedidos\/\d+$/.test(ruta) || ruta.includes("/usuario/"));

        animarFlujo(destino, haySaltoInterno);
        marcarFlujo("activa", "petición en curso…");

        const inicio = performance.now();
        let codigo = 0;
        let datos = null;

        try {
            const respuesta = await fetch(ruta, {
                method: metodo,
                headers: cuerpo ? { "Content-Type": "application/json" } : undefined,
                body: cuerpo ? JSON.stringify(cuerpo) : undefined,
            });

            codigo = respuesta.status;

            // 204 No Content no trae cuerpo que parsear.
            if (codigo !== 204) {
                const texto = await respuesta.text();
                datos = texto ? JSON.parse(texto) : null;
            }

            const ms = Math.round(performance.now() - inicio);
            registrar(metodo, ruta, codigo, ms);

            if (haySaltoInterno && respuesta.ok) {
                registrarSaltoInterno();
            }

            marcarFlujo(respuesta.ok ? "exito" : "error", `${codigo} · ${ms} ms`);
            return { ok: respuesta.ok, codigo, datos };

        } catch (error) {
            // Fallo de red: el gateway mismo no responde.
            const ms = Math.round(performance.now() - inicio);
            registrar(metodo, ruta, 0, ms);
            marcarFlujo("error", "sin conexión con el gateway");
            return { ok: false, codigo: 0, datos: null };
        }
    }

    // ---------------------------------------------------------------
    // Animación del diagrama
    // ---------------------------------------------------------------

    /** Enciende las rutas implicadas y lanza la partícula por ellas. */
    function animarFlujo(destino, haySaltoInterno) {
        const viaServicio = destino === "pedidos" ? vias.gatewayPedidos : vias.gatewayUsuarios;
        const nodoServicio = destino === "pedidos" ? nodos.pedidos : nodos.usuarios;

        encender([vias.navegadorGateway, viaServicio], [nodos.navegador, nodos.gateway, nodoServicio]);
        recorrer(vias.navegadorGateway, 340, false);
        setTimeout(() => recorrer(viaServicio, 420, false), 300);

        if (haySaltoInterno) {
            // El salto Pedidos -> Usuarios ocurre después de que la petición
            // llegó al servicio de Pedidos.
            setTimeout(() => {
                encender([vias.pedidosUsuarios], [nodos.usuarios]);
                recorrer(vias.pedidosUsuarios, 460, true);
            }, 700);
        }

        const total = haySaltoInterno ? 1500 : 900;
        setTimeout(apagarTodo, total);
    }

    function encender(rutas, nodosImplicados) {
        rutas.forEach((v) => v.classList.add("via--encendida"));
        nodosImplicados.forEach((n) => n.classList.add("nodo--activo"));
    }

    function apagarTodo() {
        Object.values(vias).forEach((v) => v.classList.remove("via--encendida"));
        Object.values(nodos).forEach((n) => n.classList.remove("nodo--activo"));
    }

    /**
     * Mueve la partícula a lo largo de una ruta SVG.
     * Usa getPointAtLength, que funciona con cualquier path sin depender de
     * librerías externas.
     */
    function recorrer(ruta, duracionMs, esInterna) {
        const largo = ruta.getTotalLength();
        const inicio = performance.now();

        dom.particula.classList.toggle("particula--interna", Boolean(esInterna));
        dom.particula.setAttribute("opacity", "1");

        function paso(ahora) {
            const avance = Math.min((ahora - inicio) / duracionMs, 1);
            const punto = ruta.getPointAtLength(avance * largo);
            dom.particula.setAttribute("cx", punto.x);
            dom.particula.setAttribute("cy", punto.y);

            if (avance < 1) {
                requestAnimationFrame(paso);
            } else {
                dom.particula.setAttribute("opacity", "0");
            }
        }

        requestAnimationFrame(paso);
    }

    function marcarFlujo(estado, texto) {
        dom.etiquetaFlujo.textContent = texto;
        dom.etiquetaFlujo.className = "etiqueta etiqueta--" + estado;

        if (estado !== "activa") {
            setTimeout(() => {
                dom.etiquetaFlujo.textContent = "en reposo";
                dom.etiquetaFlujo.className = "etiqueta";
            }, 2600);
        }
    }

    // ---------------------------------------------------------------
    // Consola de peticiones
    // ---------------------------------------------------------------

    function registrar(metodo, ruta, codigo, ms) {
        const vacia = dom.consola.querySelector(".consola__vacia");
        if (vacia) vacia.remove();

        const hora = new Date().toLocaleTimeString("es-CO", { hour12: false });
        const claseCodigo = codigo >= 200 && codigo < 400 ? "ok" : "error";
        const textoCodigo = codigo === 0 ? "ERR" : codigo;

        const linea = document.createElement("div");
        linea.className = "linea";
        linea.innerHTML = `
            <span class="linea__hora">${hora}</span>
            <span class="linea__metodo">${metodo}</span>
            <span class="linea__ruta">${ruta}</span>
            <span class="linea__codigo linea__codigo--${claseCodigo}">${textoCodigo}</span>
            <span class="linea__ms">${ms} ms</span>`;

        dom.consola.prepend(linea);
        podarConsola();
    }

    /** Anota el salto interno Pedidos -> Usuarios, que el navegador no ve. */
    function registrarSaltoInterno() {
        const linea = document.createElement("div");
        linea.className = "linea linea--interna";
        linea.innerHTML = `
            <span class="linea__hora">└─</span>
            <span class="linea__metodo">GET</span>
            <span class="linea__ruta">Pedidos → Usuarios · /api/usuarios/{id}</span>
            <span class="linea__codigo linea__codigo--ok">REST</span>
            <span class="linea__ms">interno</span>`;

        dom.consola.prepend(linea);
        podarConsola();
    }

    /** Mantiene la consola acotada para que no crezca sin límite. */
    function podarConsola() {
        const lineas = dom.consola.querySelectorAll(".linea");
        for (let i = 60; i < lineas.length; i++) {
            lineas[i].remove();
        }
    }

    // ---------------------------------------------------------------
    // Aviso flotante
    // ---------------------------------------------------------------

    let temporizadorAviso = null;

    function avisar(mensaje, tipo = "exito") {
        clearTimeout(temporizadorAviso);
        dom.aviso.textContent = mensaje;
        dom.aviso.className = `aviso aviso--visible aviso--${tipo}`;
        temporizadorAviso = setTimeout(() => {
            dom.aviso.className = "aviso";
        }, 4200);
    }

    // ---------------------------------------------------------------
    // Servicio de Usuarios
    // ---------------------------------------------------------------

    async function cargarUsuarios() {
        const { ok, datos } = await pedir("GET", "/api/usuarios");
        if (!ok || !Array.isArray(datos)) {
            dom.listaUsuarios.innerHTML =
                '<p class="lista__vacia">No se pudo consultar el servicio de Usuarios.</p>';
            dom.selectorUsuario.innerHTML = "";
            return;
        }

        pintarUsuarios(datos);
        llenarSelector(datos);
    }

    function pintarUsuarios(usuarios) {
        if (usuarios.length === 0) {
            dom.listaUsuarios.innerHTML = '<p class="lista__vacia">No hay usuarios registrados.</p>';
            return;
        }

        dom.listaUsuarios.innerHTML = usuarios
            .map(
                (u) => `
                <div class="fila">
                    <div class="fila__datos">
                        <div class="fila__titulo">
                            <span class="identificador">#${u.id}</span>${escapar(u.nombre)}
                        </div>
                        <div class="fila__meta">${escapar(u.correo)}${
                    u.ciudad ? " · " + escapar(u.ciudad) : ""
                }</div>
                    </div>
                    <button class="boton-eliminar" data-eliminar-usuario="${u.id}">Eliminar</button>
                </div>`
            )
            .join("");
    }

    function llenarSelector(usuarios) {
        const seleccionPrevia = dom.selectorUsuario.value;

        dom.selectorUsuario.innerHTML = usuarios
            .map((u) => `<option value="${u.id}">#${u.id} — ${escapar(u.nombre)}</option>`)
            .join("");

        if (seleccionPrevia && usuarios.some((u) => String(u.id) === seleccionPrevia)) {
            dom.selectorUsuario.value = seleccionPrevia;
        }
    }

    async function crearUsuario(evento) {
        evento.preventDefault();

        const cuerpo = {
            nombre: $("usuarioNombre").value.trim(),
            correo: $("usuarioCorreo").value.trim(),
            ciudad: $("usuarioCiudad").value.trim(),
        };

        const { ok, datos } = await pedir("POST", "/api/usuarios", cuerpo);

        if (ok) {
            avisar(`Usuario "${datos.nombre}" creado con id ${datos.id}.`);
            dom.formUsuario.reset();
            cargarUsuarios();
        } else {
            avisar(datos?.mensaje ?? "No se pudo crear el usuario.", "error");
        }
    }

    async function eliminarUsuario(id) {
        const { ok, datos } = await pedir("DELETE", `/api/usuarios/${id}`);

        if (ok) {
            avisar(`Usuario #${id} eliminado. Los pedidos que lo referencian ahora darán 404.`);
            cargarUsuarios();
        } else {
            avisar(datos?.mensaje ?? "No se pudo eliminar el usuario.", "error");
        }
    }

    // ---------------------------------------------------------------
    // Servicio de Pedidos
    // ---------------------------------------------------------------

    async function cargarPedidos() {
        const { ok, datos } = await pedir("GET", "/api/pedidos");
        if (!ok || !Array.isArray(datos)) {
            dom.listaPedidos.innerHTML =
                '<p class="lista__vacia">No se pudo consultar el servicio de Pedidos.</p>';
            return;
        }

        pintarPedidos(datos);
    }

    function pintarPedidos(pedidos) {
        if (pedidos.length === 0) {
            dom.listaPedidos.innerHTML = '<p class="lista__vacia">Aún no hay pedidos.</p>';
            return;
        }

        dom.listaPedidos.innerHTML = pedidos
            .map(
                (p) => `
                <div class="fila">
                    <div class="fila__datos">
                        <div class="fila__titulo">
                            <span class="identificador">#${p.id}</span>${escapar(p.producto)}
                        </div>
                        <div class="fila__meta">
                            usuario ${p.usuarioId} · ${p.cantidad} und · ${moneda(p.total)}
                        </div>
                    </div>
                    <button class="boton-eliminar" data-ver-pedido="${p.id}">Ver usuario</button>
                </div>`
            )
            .join("");
    }

    async function crearPedido(evento) {
        evento.preventDefault();

        if (!dom.selectorUsuario.value) {
            avisar("Primero debe existir al menos un usuario.", "error");
            return;
        }

        const cuerpo = {
            usuarioId: Number(dom.selectorUsuario.value),
            producto: $("pedidoProducto").value.trim(),
            cantidad: Number($("pedidoCantidad").value),
            total: Number($("pedidoTotal").value),
        };

        const { ok, datos } = await pedir("POST", "/api/pedidos", cuerpo);

        if (ok) {
            avisar(
                `Pedido #${datos.pedido.id} creado. Pedidos validó a "${datos.usuario.nombre}" llamando al servicio de Usuarios.`
            );
            dom.formPedido.reset();
            $("pedidoCantidad").value = 1;
            $("pedidoTotal").value = 250000;
            cargarPedidos();
        } else {
            avisar(datos?.mensaje ?? "No se pudo crear el pedido.", "error");
        }
    }

    /** Consulta un pedido concreto: obliga a Pedidos a llamar a Usuarios. */
    async function verPedidoConUsuario(id) {
        const { ok, datos } = await pedir("GET", `/api/pedidos/${id}`);

        if (ok) {
            avisar(
                `Pedido #${id}: "${datos.pedido.producto}" pertenece a ${datos.usuario.nombre} (${datos.usuario.correo}). Esos datos vinieron del servicio de Usuarios.`
            );
        } else {
            avisar(datos?.mensaje ?? "No se pudo consultar el pedido.", "error");
        }
    }

    /** Escenario de prueba: usuario que no existe, debe responder 404. */
    async function ensayarUsuarioInexistente() {
        const { codigo, datos } = await pedir("POST", "/api/pedidos", {
            usuarioId: 999,
            producto: "Producto de prueba",
            cantidad: 1,
            total: 100000,
        });

        if (codigo === 404) {
            avisar(`Correcto: ${datos.mensaje} El pedido no se creó.`, "error");
        } else if (codigo === 503) {
            avisar(`El servicio de Usuarios está caído: ${datos.mensaje}`, "error");
        } else {
            avisar(`Respuesta inesperada: ${codigo}`, "error");
        }
    }

    // ---------------------------------------------------------------
    // Estado de salud de los servicios
    // ---------------------------------------------------------------

    async function refrescarEstado() {
        try {
            const respuesta = await fetch("/api/sistema/estado");
            const datos = await respuesta.json();

            let vivos = 0;
            datos.servicios.forEach((servicio) => {
                const punto =
                    servicio.nombre === "Usuarios" ? salud.usuarios : salud.pedidos;
                punto.classList.toggle("nodo__salud--vivo", servicio.disponible);
                punto.classList.toggle("nodo__salud--caido", !servicio.disponible);
                if (servicio.disponible) vivos++;
            });

            const punto = dom.estadoGlobal.querySelector(".punto");
            punto.className = "punto " + (vivos === 2 ? "punto--vivo" : "punto--caido");
            dom.estadoGlobalTexto.textContent =
                vivos === 2 ? "2 / 2 servicios activos" : `${vivos} / 2 servicios activos`;

        } catch {
            dom.estadoGlobal.querySelector(".punto").className = "punto punto--caido";
            dom.estadoGlobalTexto.textContent = "gateway sin respuesta";
        }
    }

    // ---------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------

    /** Evita inyección de HTML al pintar datos que vienen del servidor. */
    function escapar(texto) {
        const div = document.createElement("div");
        div.textContent = texto ?? "";
        return div.innerHTML;
    }

    function moneda(valor) {
        return new Intl.NumberFormat("es-CO", {
            style: "currency",
            currency: "COP",
            maximumFractionDigits: 0,
        }).format(valor ?? 0);
    }

    // ---------------------------------------------------------------
    // Arranque
    // ---------------------------------------------------------------

    dom.formUsuario.addEventListener("submit", crearUsuario);
    dom.formPedido.addEventListener("submit", crearPedido);
    $("botonUsuarioInexistente").addEventListener("click", ensayarUsuarioInexistente);
    $("botonRecargar").addEventListener("click", () => {
        refrescarEstado();
        cargarUsuarios();
        cargarPedidos();
    });
    $("botonLimpiarConsola").addEventListener("click", () => {
        dom.consola.innerHTML =
            '<p class="consola__vacia">Consola limpia. Cada petición que hagas aparecerá aquí.</p>';
    });

    // Delegación de eventos: las filas se repintan, así que no se puede
    // enganchar el listener a cada botón individualmente.
    document.addEventListener("click", (evento) => {
        const eliminar = evento.target.closest("[data-eliminar-usuario]");
        if (eliminar) {
            eliminarUsuario(eliminar.dataset.eliminarUsuario);
            return;
        }

        const ver = evento.target.closest("[data-ver-pedido]");
        if (ver) {
            verPedidoConUsuario(ver.dataset.verPedido);
        }
    });

    refrescarEstado();
    cargarUsuarios();
    cargarPedidos();

    // Sonda periódica: si se apaga un servicio, el diagrama lo refleja solo.
    setInterval(refrescarEstado, 5000);
})();
