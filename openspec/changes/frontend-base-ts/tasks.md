# Tasks

## 1. Proyecto Vite en `frontend/`

- [x] 1.1 Crear `frontend/` con la plantilla `vanilla-ts` de Vite, quitar el contenido de ejemplo (contador, logos) y declarar `engines.node >= 20.19` en `package.json`; verificar que `npm install` termina sin errores y que se genera `package-lock.json` *(Desvío: la plantilla actual trae Vite 8.3 y TypeScript 7.0, no 5.x como dice la propuesta. Cumple el mínimo de la KB (TypeScript 5.x o superior) y se dejó así. También se quitó el favicon de ejemplo y se puso un ícono vacío, para que el navegador no pida `/favicon.ico` y no aparezca un 404 en la consola.)*
- [x] 1.2 Configurar `tsconfig.json` con `strict`, `noUncheckedIndexedAccess` y `noImplicitAny`, y los scripts `dev`, `build` (`tsc --noEmit && vite build`) y `test` (`vitest run`); verificar que `npm run build` genera `dist/` y que `git status` no muestra `node_modules/` ni `dist/`
- [x] 1.3 Agregar `vitest` y `jsdom` como dependencias de desarrollo, `vite.config.ts` con `test.environment = 'jsdom'` y un test trivial; verificar que `npm test` corre y pasa, y después borrar el test trivial
- [x] 1.4 Crear `frontend/.env.example` con `VITE_API_BASE_URL=http://localhost:8080/api/v1` y `src/vite-env.d.ts` con el tipo de esa variable; verificar con `git check-ignore` que `.env.example` no está ignorado y que `.env` sí

## 2. Cliente de API (`frontend-cliente-api`)

- [x] 2.1 Implementar `src/shared/api/errores.ts` (`ErrorApi`, `ErrorNegocio`, `ErrorValidacion`, `ErrorGenerico`, design D4) y los *type guards* del `ProblemDetail`; verificar que `npm run build` compila y que `grep -rnE ":\s*any\b|as any" src` no devuelve resultados
- [x] 2.2 Implementar en `src/shared/api/cliente.ts` el armado de la URL (D7), `credentials: 'include'` y el cuerpo JSON; escribir en `test/cliente.test.ts` los escenarios "Pedido a un recurso de la API", "URL base con barra final", "Pedido con la cookie de sesión", "Respuesta exitosa con datos" y "Respuesta exitosa sin cuerpo"; verificar que pasan
- [x] 2.3 Agregar el token CSRF (D5) y escribir los escenarios "POST con token CSRF", "GET sin token CSRF" y "POST sin cookie CSRF"; verificar que pasan
- [x] 2.4 Agregar la traducción de errores y escribir los escenarios "Solapamiento por profesional", "Conflicto sin código", "Inicio en el pasado", "Error interno del servidor" y "Servidor inalcanzable"; verificar que pasan y que hay 13 tests en `cliente.test.ts`

## 3. Layout y navegación (`frontend-navegacion`)

- [x] 3.1 Crear las páginas `src/pages/{agenda,reserva,configuracion}.ts` (solo el título) y `src/shared/ui/router.ts` con rutas por hash (D3); escribir en `test/navegacion.test.ts` los escenarios "Abrir la página de reserva por su dirección", "Abrir la aplicación sin dirección" y "Dirección desconocida"; verificar que pasan
- [x] 3.2 Crear `src/shared/ui/layout.ts` con el encabezado, el menú con `aria-current` y `main.ts` que los une *(Desvío: la unión vive en `src/app.ts` (`iniciarApp`) y `main.ts` solo la llama, para poder probarla sin importar los estilos)*; escribir los escenarios "Ir de Agenda a Configuración" y "Volver con el botón Atrás"; verificar que pasan
- [x] 3.3 Agregar el botón del menú con `aria-expanded`/`aria-controls` y `styles.css` mobile first con el corte en 768 px (D8); escribir los escenarios "Abrir el menú en el celular", "Elegir una página en el celular" y "Menú siempre visible en la computadora" (la parte que jsdom puede comprobar); verificar que pasan y que hay 8 tests en `navegacion.test.ts`
- [x] 3.4 Levantar `npm run dev`, capturar la aplicación a 390 × 844 (menú cerrado y abierto) y a 1440 × 900 en un navegador real, guardar las capturas en `openspec/changes/frontend-base-ts/verify/` y verificar que en los dos anchos no hay desplazamiento horizontal ni errores en la consola

## 4. Verificación integral

- [x] 4.1 Correr `npm run build` y `npm test` en `frontend/` y verificar que los 21 tests (13 + 8, uno por escenario, con el nombre igual al escenario) pasan en verde
- [x] 4.2 Verificar que `frontend/` no importa ningún framework de UI (revisando `package.json`) y que `grep -rnE ":\s*any\b|as any" frontend/src frontend/test` no devuelve resultados
- [x] 4.3 Verificar que los tests y las capturas usan solo datos ficticios y que no hay URLs ni tokens reales en el código
- [x] 4.4 Correr `openspec validate frontend-base-ts --strict` y verificar que es válido

## Workflow follow-up

- Confirmar con Juan Sebastián (C-05) el formato del CSRF y con Sofia (C-06) la propiedad `codigo` del `ProblemDetail`, y anotar la respuesta en el PR.
- Abrir el PR de `change/C-02-frontend-base-ts` a `master` con la evidencia de tests y capturas, y marcar `[x]` en C-02 de `CHANGES.md`.
- Archivar el change después de que la líder lo revise y lo mergee.
