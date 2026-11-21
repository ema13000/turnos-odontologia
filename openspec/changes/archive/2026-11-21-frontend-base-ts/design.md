# Design

## Context

- En `master` (`fa98610`) todavía no existe `frontend/`. Este change la crea desde cero. La motivación está en `proposal.md` (Why) y los requisitos en `specs/frontend-cliente-api/spec.md` y `specs/frontend-navegacion/spec.md`.
- La estructura de carpetas acordada está en `knowledge-base/08_arquitectura_propuesta.md`: `src/features/{agenda,reserva,configuracion}/`, `src/shared/{api,ui,validacion}/` y `src/pages/`.
- Reglas duras que condicionan el diseño: TypeScript `strict`, sin `any` ni frameworks (regla 10); sesión con cookie y CSRF activo, sin JWT (regla 8, DD-10); un test por escenario (regla 7); solo datos ficticios (regla 1).
- El backend todavía no expone endpoints. La sesión y el CSRF los define C-05 (Juan Sebastián) y el formato de los errores, C-06 (Sofia). Por eso el cliente se prueba con `fetch` simulado.
- La política visual del proyecto es `off`: no hay design system ni maquetas que respetar.
- En la máquina de Enzo hay Node 24.21 y npm 11.19.

## Goals / Non-Goals

**Goals:**
- Que `npm run build` y `npm test` funcionen en verde en cualquier máquina con Node 20.19 o superior, sin backend levantado.
- Que el cliente de API sea el único lugar que conoce la URL base, la cookie y el CSRF. Las pantallas de C-09 y C-12 solo llaman a sus funciones.
- Trazabilidad 1 a 1 entre cada escenario del spec y su test.

**Non-Goals:**
- Reintentos, cache o cancelación de pedidos.
- Internacionalización: los textos van en español y escritos en el código.
- Un sistema de componentes de UI: solo el layout y la navegación mínimos.

## Decisions

### D1. Estructura de `frontend/`

```
frontend/
  package.json, package-lock.json, tsconfig.json, vite.config.ts
  index.html, .env.example
  src/
    main.ts                     arma el layout e inicia el router
    styles.css                  estilos base mobile first
    vite-env.d.ts               tipos de import.meta.env (VITE_API_BASE_URL)
    pages/                      agenda.ts, reserva.ts, configuracion.ts (solo el título)
    shared/
      api/cliente.ts            solicitar(), obtener(), enviar()...
      api/errores.ts            ErrorApi, ErrorNegocio, ErrorValidacion, ErrorGenerico
      ui/layout.ts              encabezado, menú y botón del menú
      ui/router.ts              rutas por hash
  test/                         *.test.ts, uno por archivo de spec
```

`src/features/` y `src/shared/validacion/` se crean cuando C-09 y C-12 los necesiten. Carpetas vacías no aportan nada.

### D2. Vite con `vanilla-ts` y Vitest para los tests

Se usa Vite (confirma SU-05) y **Vitest** con entorno `jsdom`, que reutiliza la configuración de Vite y corre TypeScript sin un paso aparte de compilación. `npm test` ejecuta `vitest run`. `npm run build` ejecuta `tsc --noEmit && vite build`, así un error de tipos rompe el build.

*Alternativas*: Jest necesita `ts-jest` o Babel y configuración aparte. El `node:test` nativo no tiene DOM para probar la navegación.

### D3. Navegación por hash (`#/agenda`)

El router escucha `hashchange` y renderiza la página que corresponde. Con un hash vacío o desconocido, reemplaza la dirección por `#/agenda` con `history.replaceState`, para que Atrás no vuelva a una dirección inválida.

*Alternativa*: la History API con rutas limpias (`/agenda`). Necesita que el servidor devuelva `index.html` para cualquier ruta, y esa configuración de despliegue todavía no existe (PA-11). Migrar después solo cambia `router.ts`.

### D4. Cliente de API y errores tipados

`solicitar<T>(metodo, ruta, cuerpo?)` devuelve `Promise<T | undefined>`. Por encima hay atajos (`obtener`, `crear`, `actualizar`, `borrar`). Para los errores lanza una jerarquía de clases:

- `ErrorNegocio` (409), con `codigo` y `detalle`;
- `ErrorValidacion` (422), con `detalle` y `codigo` opcional;
- `ErrorGenerico`, con `estado` (`number | null`) y un mensaje fijo.

Todas heredan de `ErrorApi`, así una pantalla puede usar `instanceof` y `switch (error.codigo)`. El cuerpo de error se lee como `unknown` y se valida con *type guards*, sin `any` ni casteos. Si el JSON está mal formado, se trata como si faltara el código.

*Alternativa*: devolver un resultado `{ ok, error }` en lugar de lanzar. Obliga a chequear cada llamada; con `async/await` y `try/catch` el uso es más simple y los errores no esperados no se pierden.

### D5. CSRF con la convención de Spring Security (supuesto con C-05)

El cliente lee la cookie `XSRF-TOKEN` y envía su valor en `X-XSRF-TOKEN` en `POST`, `PUT`, `PATCH` y `DELETE`. Es lo que espera Spring Security con `CookieCsrfTokenRepository.withHttpOnlyFalse()` y el manejador para SPA que recomienda su documentación. La cookie de **sesión** sigue siendo `HttpOnly`. Solo el token CSRF es legible, y por diseño no sirve sin la sesión.

*Alternativa*: un endpoint que devuelva el token en JSON. Agrega un pedido extra y algo más que C-05 tendría que exponer. Si C-05 elige otro nombre, el cambio se limita a dos constantes de `cliente.ts`.

### D6. Código de negocio en `ProblemDetail.codigo` (supuesto con C-06)

C-01 define los códigos (`CodigoRechazo`) y deja a C-06 la traducción a `ProblemDetail`. El cliente lee la propiedad de extensión `codigo`, en español como el resto del contrato. Si falta, usa `CONFLICTO_SIN_CODIGO`, para que la pantalla siempre tenga un código con el que decidir. Hay que confirmarlo con Sofia antes de que C-06 implemente su manejador de errores.

### D7. URL base desde `import.meta.env`

`VITE_API_BASE_URL` se lee una vez en `cliente.ts` y se normaliza quitando la barra final. Las rutas empiezan siempre con `/`. Para los tests, `cliente.ts` exporta una función que arma el cliente con una URL base dada, sin depender de variables de entorno. En `.env.example` va el ejemplo de la KB (`http://localhost:8080/api/v1`).

### D8. Layout responsivo con CSS propio

`styles.css` es mobile first, con variables CSS locales (colores, espacios) y un único corte en `min-width: 768px`. Debajo del corte, el menú se oculta y aparece un botón con `aria-expanded` y `aria-controls`. Desde el corte, el menú se muestra en línea y el botón se oculta. Lo que hace JavaScript (abrir, cerrar, cerrar al elegir) está en `layout.ts` y se prueba con jsdom. Lo visual (qué se ve en cada ancho, que no haya desplazamiento horizontal) depende de las media queries, que jsdom no evalúa. Esa parte se verifica con capturas en un navegador real a 390 × 844 y 1440 × 900, que se guardan en `openspec/changes/frontend-base-ts/verify/`.

### D9. Trazabilidad de los tests

Cada test de Vitest se llama exactamente como su escenario (`it('Solapamiento por profesional', ...)`), agrupado con `describe` por requisito. `test/cliente.test.ts` cubre `frontend-cliente-api` y `test/navegacion.test.ts` cubre `frontend-navegacion`. `fetch` se reemplaza con `vi.fn()` y la cookie se fija con `document.cookie`, siempre con datos ficticios.

## Risks / Trade-offs

- **[CORS con credenciales]** Con el frontend en `localhost:5173` y la API en `localhost:8080`, el backend tiene que permitir ese origen con `allowCredentials`. → Se anota en el PR para C-05 y C-06. Si molesta, `vite.config.ts` puede agregar un proxy de `/api` y usar `VITE_API_BASE_URL=/api/v1`, sin cambiar el cliente.
- **[Supuestos de contrato]** Si C-05 o C-06 eligen otros nombres para el CSRF o el código, el cliente falla en silencio contra el backend real. → Las constantes están en un solo lugar, y el PR pide la confirmación de Juan Sebastián y de Sofia.
- **[Escenarios visuales sin test automático]** Las media queries no corren en jsdom (regla 7). → El comportamiento del botón sí tiene test, y el resultado visual se verifica con capturas guardadas como evidencia. Si la líder exige un test automático también para esa parte, se puede sumar Playwright en un change aparte.
- **[Versiones de dependencias]** Vite 7 pide Node 20.19 o superior. → `package.json` declara `engines.node` y `package-lock.json` fija las versiones exactas.
