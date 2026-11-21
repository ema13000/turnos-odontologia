# Tasks

> Governance **CRÍTICO**: no empezar el grupo 1 hasta que la líder apruebe `proposal.md`, los specs y `design.md`, incluidas las decisiones *(a confirmar)*. Strict TDD: en cada tarea de comportamiento, primero el test en rojo (RED), después el mínimo código para pasarlo (GREEN), y después el refactor con los tests en verde. Cada test de escenario lleva `@DisplayName` igual al nombre del escenario del spec. Solo datos ficticios.

## 0. Aprobación previa

- [x] 0.1 Registrar en el PR o en `design.md` la respuesta de la líder a las decisiones *(a confirmar)* y a las Open Questions; si alguna cambia, actualizar los specs y estas tareas con `openspec-update-change` antes de seguir; verificar con `openspec validate auth-sesion-roles --strict`
- [x] 0.2 Avisar en el PR a Sofia (`build.gradle`), a Juan Cruz (`PacienteEntity`, `.env.example`) y a Emanuel (posible conflicto con C-04 en `build.gradle`); verificar que el aviso figura en la descripción del PR

## 1. Dependencias y configuración base

- [x] 1.1 Safety net: correr `./gradlew test` e `./gradlew integrationTest` en la rama sin cambios y anotar el baseline (cantidad de tests en verde); si algo falla, frenar y reportarlo como falla previa
- [x] 1.2 Agregar a `build.gradle` `spring-boot-starter-web`, `spring-boot-starter-validation`, `spring-boot-starter-security` y `testImplementation 'org.springframework.security:spring-security-test'`, sin versiones explícitas (las fija el BOM); verificar que `./gradlew compileJava compileTestJava` termina en verde
- [x] 1.3 Agregar a `application.yml` las propiedades de sesión, cookie, CORS y admin inicial de design D9, y a `.env.example` las variables nuevas con valores de ejemplo (sin contraseñas); verificar con `git diff .env.example` que no hay secretos

## 2. Migración V3 y persistencia de usuarios

- [x] 2.1 RED: escribir `UsuarioRestriccionesIntegrationTest` (`@Tag("integration")`, JDBC como C-03) con casos para: username duplicado, username sin normalizar, rol inválido, `PACIENTE` sin `paciente_id`, staff con `paciente_id`, `ODONTOLOGO` sin `profesional_id`, `RECEPCIONISTA` con `profesional_id`, dos usuarios para el mismo paciente; y un caso válido por rol; verificar que falla porque la tabla no existe
- [x] 2.2 GREEN: crear `V3__usuarios.sql` según design D6; verificar que `UsuarioRestriccionesIntegrationTest` pasa y que `RestriccionesTurnoIntegrationTest` sigue en verde
- [x] 2.3 Crear `UsuarioEntity` y `UsuarioJpaRepository` en `usuarios/infrastructure` (con `ddl-auto: validate`); verificar que el contexto arranca con `TurnosOdontologiaApplicationTest`

## 3. Dominio de usuarios

- [x] 3.1 RED → GREEN: `NombreUsuarioTest` (trim y minúsculas; ` Recepcion ` → `recepcion`; vacío rechazado) e implementación de `NombreUsuario`; verificar con `./gradlew test --tests '*NombreUsuarioTest'`
- [x] 3.2 RED → GREEN: `PoliticaContrasenaTest` con 7, 8, 72 y 73 caracteres e implementación de `PoliticaContrasena`; verificar con `./gradlew test --tests '*PoliticaContrasenaTest'`
- [x] 3.3 Crear `Rol` y verificar con `grep` que `usuarios/domain` no importa `org.springframework` ni `jakarta.persistence`

## 4. Autenticación: login, sesión, CSRF y logout (spec `seguridad-autenticacion`)

- [x] 4.1 RED: en `AutenticacionIntegrationTest` (`@Tag("integration")`, `MockMvc`), escribir "Sin sesión iniciada" y "Login correcto del personal", obteniendo el token con `GET /public/sesion` (sin `.with(csrf())`); verificar que fallan
- [x] 4.2 GREEN: `SecurityConfig` (CSRF con cookie y handler SPA, `CsrfCookieFilter`, sin `formLogin` ni `httpBasic`, `PasswordEncoder` BCrypt), `UsuarioAutenticado`, `UsuarioDetailsService`, `AutenticacionController` (`/public/login` y `/public/sesion`) con la estrategia de sesión de design D2; verificar que los 2 escenarios pasan
- [x] 4.3 RED → GREEN: "Login con el usuario en mayúsculas" y "Login correcto de un paciente"; verificar que pasan
- [x] 4.4 RED → GREEN: "Contraseña incorrecta", "Usuario inexistente" y "Usuario inactivo" con la traducción de `AuthenticationException` a `401 CREDENCIALES_INVALIDAS` en `UsuariosExceptionHandler` (mismo `detail` para los tres); verificar que pasan
- [x] 4.5 RED → GREEN: "Login sin contraseña" con `UsuariosExceptionHandler` (`422 DATOS_INVALIDOS` + `errores`); verificar que pasa
- [x] 4.6 RED → GREEN: "Login sin token CSRF", "Token CSRF que no coincide" y "Token CSRF renovado al iniciar sesión" (`403 CSRF_INVALIDO`); verificar que pasan
- [x] 4.7 RED → GREEN: "Nuevo identificador de sesión al iniciar sesión"; verificar que pasa
- [x] 4.8 RED → GREEN: "Logout con sesión iniciada" y "Logout de un paciente" (`POST /api/v1/logout` → `204`, sesión invalidada, cookie borrada); verificar que pasan
- [x] 4.9 RED → GREEN: en `CookieSesionIntegrationTest` (`RANDOM_PORT` + `RestClient`), "Atributos de la cookie de sesión" y "Cookie segura en producción" (con `SESSION_COOKIE_SECURE=true` vía `@TestPropertySource` en una clase aparte); verificar que pasan
- [x] 4.10 RED → GREEN: "Hash guardado al crear un usuario" (hash BCrypt distinto de la contraseña y que verifica con `PasswordEncoder.matches`); verificar que pasa
- [x] 4.11 REFACTOR: revisar que ningún log ni respuesta incluya contraseñas o hashes (`grep` de `password` en los `log.` del código nuevo); verificar que `./gradlew test integrationTest` sigue en verde

## 5. Autorización por rol y aislamiento del paciente (spec `seguridad-autorizacion`)

- [x] 5.1 RED: en `AutorizacionIntegrationTest`, escribir "Ruta pública sin sesión", "Ruta del personal sin sesión" y "Ruta del paciente sin sesión"; verificar que los de `401` fallan
- [x] 5.2 GREEN: reglas de la cadena de filtros de design D4 y `ProblemDetailAuthenticationEntryPoint` (`401 NO_AUTENTICADO`, sin redirección); verificar que los 3 pasan
- [x] 5.3 RED → GREEN: "El personal no usa las rutas del paciente", "Un paciente pide la agenda del personal" y "Un paciente pide los datos de otro paciente por la ruta del personal" (`403 ACCESO_DENEGADO`); verificar que pasan
- [x] 5.4 RED → GREEN: controller solo de test en `src/test` (`/api/v1/prueba-seguridad/solo-admin`, `@PreAuthorize("hasRole('ADMIN')")`) y `@EnableMethodSecurity`; escenarios "Operación de ADMIN pedida por recepción" y "Operación de ADMIN pedida por el administrador"; verificar que pasan
- [x] 5.5 RED → GREEN: "Ruta fuera de la API con sesión del administrador" (`denyAll`); verificar que pasa
- [x] 5.6 RED → GREEN: `PerfilPacienteController` (`GET /api/v1/paciente/perfil` con `@AuthenticationPrincipal`, sin ids del pedido) y los escenarios "Perfil propio" e "Intento de pedir el perfil de otro paciente"; verificar que pasan
- [x] 5.7 RED → GREEN: `CorsConfigurationSource` con `app.seguridad.cors-origenes`; escenarios "Origen configurado" y "Origen no configurado"; verificar que pasan y que el código no contiene `"*"` como origen

## 6. Registro del paciente (spec `cuentas-pacientes`)

- [x] 6.1 RED → GREEN (unitario): `RegistrarPacienteTest` con repositorios en memoria en `src/test` para DNI existente, email existente (sin distinguir mayúsculas) y registro correcto; implementar `RegistrarPaciente`, los puertos `UsuarioRepository` y `PacienteRegistro`, y `RegistroRechazadoException`; verificar con `./gradlew test --tests '*RegistrarPacienteTest'`
- [x] 6.2 Agregar en `PacienteEntity` el constructor o *factory* con email y teléfono (aviso a Juan Cruz, tarea 0.2) y los adaptadores JPA de los puertos; verificar que `RestriccionesTurnoIntegrationTest` sigue en verde
- [x] 6.3 RED → GREEN: en `RegistroPacienteIntegrationTest`, "Registro correcto" y "El registro no inicia la sesión" con `RegistroPacienteController`; verificar que pasan
- [x] 6.4 RED → GREEN: "DNI de un paciente con cuenta", "DNI de un paciente cargado por el consultorio" y "Email ya usado por otra cuenta", incluida la traducción de `DataIntegrityViolationException` por nombre de restricción (design D7); verificar que pasan
- [x] 6.5 RED → GREEN: "Contraseña demasiado corta", "Contraseña en el límite mínimo", "Contraseña demasiado larga", "DNI y email con formato inválido" y "Faltan datos obligatorios"; verificar que pasan

## 7. Primer administrador (spec `seguridad-autenticacion`)

- [x] 7.1 RED → GREEN (unitario): `CrearAdminInicialTest` con repositorio en memoria y los escenarios "Primer arranque con las variables definidas", "Ya existe un administrador" y "Variables sin definir"; implementar `CrearAdminInicial` y `CrearAdminInicialRunner`; verificar con `./gradlew test --tests '*CrearAdminInicialTest'`
- [x] 7.2 Agregar un test de integración que arranca el contexto con `APP_ADMIN_USERNAME`/`APP_ADMIN_PASSWORD` ficticios y hace login con ese `admin`; verificar que pasa

## 8. Verificación integral

- [x] 8.1 Correr `./gradlew build` e `./gradlew integrationTest` con PostgreSQL levantado y verificar que hay un test por cada uno de los 43 escenarios de los tres specs (20 + 13 + 10), todos en verde, más el baseline de la tarea 1.1
- [x] 8.2 Verificar con `grep` en `backend/src/main` que no aparecen `csrf.disable`, `csrf(AbstractHttpConfigurer::disable)`, `ignoringRequestMatchers`, `jwt`, `allowedOrigins("*")`, `permitAll()` fuera de `/api/v1/public/**`, ni contraseñas literales
- [x] 8.3 Verificar que ningún controller del change devuelve una entidad JPA (los tipos de retorno son records de `usuarios/api`)
- [x] 8.4 Probar a mano con el frontend de C-02 (o `curl` con cookies): `GET /public/sesion` → login → `GET /paciente/perfil` → logout; guardar la salida en `docs/evidencia/etapa-c05-auth.txt` sin contraseñas
- [x] 8.5 Correr `openspec validate auth-sesion-roles --strict` y verificar que es válido

## Workflow follow-up

- Abrir el PR de `change/C-05-auth-sesion-roles` a `master` con los avisos de la tarea 0.2; Sofia revisa y mergea.
- Marcar `[x]` en C-05 de `CHANGES.md` con la evidencia.
- Pedir que se corrija `knowledge-base/08_arquitectura_propuesta.md` §Seguridad (paciente con cuenta, RN-RO-08) y que se asigne el ABM de usuarios del personal a un change.
- Archivar con `openspec archive auth-sesion-roles` y verificar los specs creados en `openspec/specs/`.
