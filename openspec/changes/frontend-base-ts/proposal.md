# Proposal

## Why

Hoy el repositorio no tiene frontend. Las pantallas del MVP (la agenda en C-09 y la reserva del paciente en C-12) necesitan una base común: un proyecto TypeScript estricto que compile, una navegación responsiva y una única forma de hablar con la API. Esa forma tiene que respetar la sesión con cookie y CSRF (DD-10) y entender los errores de negocio de la API (`409` con código estable, `422` para validaciones). Este change es C-02. No depende de nada y se hace en paralelo con el backend, para que C-09 y C-12 arranquen apenas exista la API.

## What Changes

- **Proyecto `frontend/`** con Vite (plantilla `vanilla-ts`), TypeScript 5.x en modo `strict`, sin frameworks ni `any`, y los scripts `dev`, `build` y `test`. Así queda validado el supuesto SU-05.
- **Layout responsivo** (mobile first) con navegación entre tres páginas vacías: `agenda`, `reserva` y `configuracion`. Se puede usar desde el celular (US-014 CA-2).
- **Cliente de API** (`src/shared/api/cliente.ts`), único punto de acceso a la API:
  - envía la cookie de sesión en cada pedido (`credentials: 'include'`);
  - agrega el token CSRF en los métodos que modifican datos;
  - convierte un `409` en un error de negocio con su código estable (`SOLAPAMIENTO_PROFESIONAL`, `SOLAPAMIENTO_SILLON`, `MARGEN_INSUFICIENTE`, etc.) y un `422` en un error de validación;
  - convierte cualquier otra falla en un error genérico que no expone detalles internos.
- **`frontend/.env.example`** con `VITE_API_BASE_URL` (ejemplo `http://localhost:8080/api/v1`).
- **Tests de unidad** con `fetch` simulado, uno por escenario del spec.

**Supuestos a confirmar con otros dueños** (se registran en `design.md`):
- **Con C-05 (Juan Sebastián)**: el token CSRF viaja como lo hace Spring Security con `CookieCsrfTokenRepository`, es decir, cookie `XSRF-TOKEN` legible y encabezado `X-XSRF-TOKEN`.
- **Con C-06 (Sofia)**: el código de negocio viaja en la propiedad `codigo` del `ProblemDetail`.

**Fuera de alcance**:
- Pantallas con datos reales: la agenda va en C-09 y la reserva del paciente en C-12.
- Login y pantallas de sesión. Este change solo prepara el cliente, y C-05 define los endpoints.
- Design system y maquetas: la política visual del proyecto es `off`.
- Despliegue y Docker (PA-11).

## Capabilities

### New Capabilities

- `frontend-cliente-api`: cómo el frontend se comunica con la API REST. Cubre la URL base configurable, el envío de la sesión y el token CSRF, y la traducción de las respuestas de error (`409`, `422` y otras) a errores tipados. Los changes de frontend posteriores la reutilizan y le suman sus endpoints.
- `frontend-navegacion`: estructura navegable y responsiva de la aplicación web. Cubre las páginas disponibles, cómo se pasa de una a otra y cómo se comporta en pantallas chicas y grandes. C-09 y C-12 suman el contenido de cada página.

### Modified Capabilities

Ninguna. `agenda-turnos` no cambia.

## Impact

- **Código nuevo**: carpeta `frontend/`, cuyo dueño es Enzo según `CHANGES.md`. No toca `backend/` ni módulos ajenos.
- **Dependencias nuevas** (solo de desarrollo, en `frontend/package.json`): `vite`, `typescript` y `vitest`, con versiones fijadas en `package-lock.json`. Necesita Node.js 20 o superior.
- **Contratos con otros changes**: el formato CSRF (C-05) y la propiedad del código de error (C-06), según los supuestos de arriba.
- **Changes que se desbloquean**: C-09 (si C-06 está listo) y C-12 (si C-10 está listo).
- **Sin migraciones**: no reserva números de Flyway.
