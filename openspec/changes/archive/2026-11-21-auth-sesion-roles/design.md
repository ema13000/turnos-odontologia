# Design

## Context

- Ver `proposal.md` (Why) para la motivación y los specs `seguridad-autenticacion`, `seguridad-autorizacion` y `cuentas-pacientes` para los requisitos.
- Estado actual del backend (observado en `master` después de C-01 y C-03):
  - `build.gradle`: Spring Boot 3.5.16, Java 21, solo `spring-boot-starter`, `data-jpa`, Flyway y el driver de PostgreSQL. **No hay** `starter-web`, `validation` ni `security`. La tarea `test` excluye `@Tag("integration")`, y `integrationTest` los incluye.
  - `application.yml`: perfil `dev` con `DB_URL`, `DB_USER`, `DB_PASSWORD`, `ddl-auto: validate` y `open-in-view: false`.
  - V1 (`V1__base_agenda.sql`): PK `BIGINT GENERATED ALWAYS AS IDENTITY` (no UUID como dice la KB), `paciente(nombre, apellido, dni UNIQUE, email, telefono)`, sin columnas de consentimiento.
  - `shared/persistence/PacienteEntity` (dueño: Juan Cruz) solo tiene un constructor con nombre, apellido y DNI.
  - Los tests de integración (`RestriccionesTurnoIntegrationTest`) usan PostgreSQL real con `DB_*` del entorno, migran con Flyway y vacían las tablas con `TRUNCATE … CASCADE`.
  - El frontend (C-02) ya asume la cookie `XSRF-TOKEN`, el encabezado `X-XSRF-TOKEN`, `credentials: 'include'` y el `ProblemDetail` con la propiedad `codigo`.
- Governance **CRÍTICO**: el diseño se revisa antes de escribir código. Las decisiones marcadas *(a confirmar)* necesitan el visto bueno de la líder.

## Goals / Non-Goals

**Goals:**
- Una sola cadena de filtros de Spring Security que deniega por defecto y que los changes siguientes (C-04, C-06, C-12, C-13) no necesitan tocar: cada uno agrega `@PreAuthorize` en sus controllers.
- Un principal de sesión que expone el `pacienteId` o el `profesionalId` del usuario, para que ningún endpoint del paciente acepte un id del pedido.
- Errores `401`/`403`/`409`/`422` con el mismo formato `ProblemDetail` + `codigo` que espera el frontend.

**Non-Goals:**
- Manejador global de errores de la API: lo define C-06. Este change solo maneja los errores de sus paquetes y de la cadena de filtros.
- Sesiones compartidas entre instancias (Spring Session): no aplica en v1 (DD-10).
- Límite de intentos de login: pendiente de decisión (ver Open Questions).

## Decisions

### D1. Paquetes y dueños

```
backend/src/main/java/ar/turnos/odontologia/
  shared/security/          (Juan Sebastián)
    SecurityConfig              cadena de filtros, CSRF, CORS, sesión, PasswordEncoder
    CsrfCookieFilter            fuerza la emisión del token CSRF diferido
    SpaCsrfTokenRequestHandler  acepta el token del encabezado sin máscara y el del formulario con máscara
    ProblemDetailAuthenticationEntryPoint   401 NO_AUTENTICADO
    ProblemDetailAccessDeniedHandler        403 ACCESO_DENEGADO / CSRF_INVALIDO
    UsuarioAutenticado          principal (record): usuarioId, username, rol, pacienteId?, profesionalId?
    UsuarioDetailsService       adaptador de UsuarioRepository a UserDetailsService
    SeguridadProperties         @ConfigurationProperties("app.seguridad"): orígenes CORS, cookie segura, admin inicial
  usuarios/                 (Juan Sebastián)
    domain/         Rol, NombreUsuario (normaliza: trim + minúsculas), PoliticaContrasena
    application/    RegistrarPaciente, CrearAdminInicial, UsuarioRepository (puerto), PacienteRegistro (puerto)
    infrastructure/ UsuarioEntity, UsuarioJpaRepository, adaptadores JPA, CrearAdminInicialRunner (ApplicationRunner)
    api/            AutenticacionController (login, sesion), RegistroPacienteController,
                    PerfilPacienteController, DTOs (records), UsuariosExceptionHandler
backend/src/main/resources/db/migration/V3__usuarios.sql
```

Sigue la arquitectura hexagonal de C-01: `usuarios/domain` no importa Spring ni JPA, y se prueba con tests unitarios. `PerfilPacienteController` vive en `usuarios/api` porque C-12 (`reserva/`) todavía no existe; si C-12 prefiere tenerlo, se mueve en ese change.

*Alternativa descartada*: poner todo en `shared/security`. Mezcla configuración transversal con un caso de uso de negocio (registro).

### D2. Login con un controller propio, no con `formLogin`

`formLogin` responde con redirecciones y espera `application/x-www-form-urlencoded`. El frontend habla JSON. Por eso `AutenticacionController.login`:

1. valida el `LoginRequest` (record con `@NotBlank username`, `@NotBlank password`) → `422` si falta algo;
2. normaliza el usuario (`NombreUsuario`) y llama a `AuthenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(...))`;
3. aplica la `SessionAuthenticationStrategy` compuesta: `ChangeSessionIdAuthenticationStrategy` (fijación de sesión) + `CsrfAuthenticationStrategy` (renueva el token CSRF);
4. guarda el `SecurityContext` con `HttpSessionSecurityContextRepository.saveContext(...)`, que es obligatorio en Spring Security 6 cuando se autentica a mano;
5. responde `200` con `SesionResponse(autenticado, username, rol)`.

`BadCredentialsException`, `DisabledException` y `UsernameNotFoundException` se traducen igual a `401 CREDENCIALES_INVALIDAS`, con el mismo `detail`. `DaoAuthenticationProvider` ya oculta el usuario inexistente (`hideUserNotFoundExceptions`) y compara contra un hash ficticio para igualar los tiempos.

*Alternativa descartada*: un filtro `AbstractAuthenticationProcessingFilter` propio para JSON. Hace lo mismo con más código de infraestructura y es más difícil de probar con `@Valid`.

### D3. CSRF con cookie legible y encabezado (convención de C-02)

- `CookieCsrfTokenRepository.withHttpOnlyFalse()`: cookie `XSRF-TOKEN`, encabezado `X-XSRF-TOKEN`.
- Spring Security 6 difiere y enmascara el token (BREACH). Para una SPA, la receta de la documentación es un `CsrfTokenRequestHandler` que acepta el valor crudo del encabezado, más un filtro que carga el token en cada respuesta para que la cookie exista. Se verifica la receta exacta para Spring Security 6.5 (Boot 3.5) con la documentación oficial antes de implementar; `csrf.spa()` existe recién en Spring Security 7.
- `GET /api/v1/public/sesion` es el punto de entrada del frontend: devuelve el estado de la sesión y deja la cookie `XSRF-TOKEN`, así el login también va con token (protege contra *login CSRF*).
- El `AccessDeniedHandler` distingue `MissingCsrfTokenException` / `InvalidCsrfTokenException` (→ `403 CSRF_INVALIDO`) del resto (→ `403 ACCESO_DENEGADO`).
- **Nunca** `csrf.disable()` ni `ignoringRequestMatchers` (regla dura 8).

### D4. Reglas de acceso en la cadena de filtros, detalle por método

```
/api/v1/public/**    permitAll
/api/v1/logout       lo atiende LogoutFilter, para cualquier sesión
/api/v1/paciente/**  hasRole(PACIENTE)
/api/v1/**           hasAnyRole(ADMIN, RECEPCIONISTA, ODONTOLOGO)
anyRequest           denyAll
```

- `@EnableMethodSecurity` en `SecurityConfig` (clase `@Configuration` aparte, no en `@SpringBootApplication`). Cada change agrega `@PreAuthorize("hasRole('ADMIN')")` etc. en sus endpoints según la matriz RBAC.
- La autorización corre antes del `DispatcherServlet`, así que `GET /api/v1/turnos` da `403` a un paciente aunque C-06 todavía no exista. Los tests de este change se apoyan en eso.
- Para probar la seguridad por método sin depender de endpoints ajenos, los tests usan un controller **solo de test** (`src/test`) en `/api/v1/prueba-seguridad/solo-admin` con `@PreAuthorize("hasRole('ADMIN')")`.
- `AuthenticationEntryPoint` propio → `401 NO_AUTENTICADO` en `ProblemDetail` (sin redirección ni `WWW-Authenticate: Basic`). `httpBasic` y `formLogin` deshabilitados.

### D5. Principal con el vínculo al paciente o al profesional

`UsuarioAutenticado` implementa `UserDetails` y lleva `usuarioId`, `username`, `rol`, `pacienteId` y `profesionalId` (los dos últimos opcionales). Los controllers de `/paciente/**` lo reciben con `@AuthenticationPrincipal` y usan `pacienteId()`; **no** declaran `@PathVariable` ni `@RequestParam` con ids de paciente. C-12 sigue el mismo patrón para verificar que un turno sea del paciente (US-024 CA-2). `GET /paciente/perfil` devuelve `PerfilPacienteResponse(nombre, apellido, dni, email, telefono)`.

### D6. Modelo de datos (V3)

```sql
CREATE TABLE usuario (
  id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  username       TEXT        NOT NULL,
  password_hash  TEXT        NOT NULL,
  rol            TEXT        NOT NULL,
  profesional_id BIGINT      REFERENCES profesional (id),
  paciente_id    BIGINT      REFERENCES paciente (id),
  activo         BOOLEAN     NOT NULL DEFAULT TRUE,
  creado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT usuario_username_unico UNIQUE (username),
  CONSTRAINT usuario_paciente_unico UNIQUE (paciente_id),
  CONSTRAINT usuario_profesional_unico UNIQUE (profesional_id),
  CONSTRAINT usuario_username_normalizado CHECK (username = lower(btrim(username)) AND length(username) BETWEEN 3 AND 150),
  CONSTRAINT usuario_rol_valido CHECK (rol IN ('ADMIN','RECEPCIONISTA','ODONTOLOGO','PACIENTE')),
  CONSTRAINT usuario_paciente_vinculo CHECK ((rol = 'PACIENTE') = (paciente_id IS NOT NULL)),
  CONSTRAINT usuario_profesional_vinculo CHECK (
    profesional_id IS NULL OR rol IN ('ODONTOLOGO','ADMIN')),
  CONSTRAINT usuario_odontologo_con_profesional CHECK (rol <> 'ODONTOLOGO' OR profesional_id IS NOT NULL)
);
```

- Mismo estilo que V1 (identidad `BIGINT`). Las `UNIQUE` crean los índices de las FK. `TEXT` + `CHECK` según la skill `postgresql-table-design`.
- El vínculo `PACIENTE ⇔ paciente_id` y "paciente nunca con profesional" (KB §Usuario) quedan garantizados en la base, además del dominio.
- `profesional_id` permitido para `ADMIN` porque el dueño puede ser el mismo odontólogo (KB §Actores). *(a confirmar)*
- `username` siempre normalizado: la búsqueda del login es por igualdad sobre la columna única, sin `lower()` en la consulta.

### D7. Registro del paciente en una transacción

`RegistrarPaciente` (servicio `@Transactional`):
1. el DTO `RegistroPacienteRequest` valida con Bean Validation: `@NotBlank @Size(max=100)` nombre y apellido, `@Pattern("\\d{7,8}")` DNI, `@NotBlank @Email @Size(max=150)` email, `@Size(max=30)` teléfono, `@NotBlank` contraseña; `PoliticaContrasena` (dominio) aplica el largo 8..72, que se reporta en el campo `password`;
2. si existe un paciente con ese DNI → `409 DNI_YA_REGISTRADO`; si existe un usuario con ese email normalizado → `409 EMAIL_YA_REGISTRADO` (el DNI se verifica primero);
3. crea el paciente (por el puerto `PacienteRegistro`, implementado sobre `PacienteEntity`) y el usuario `PACIENTE` con el hash BCrypt;
4. una `DataIntegrityViolationException` por carrera se traduce según el nombre de la restricción (`paciente_dni_key` → `DNI_YA_REGISTRADO`, `usuario_username_unico` → `EMAIL_YA_REGISTRADO`), así el resultado no depende de quién llega primero.

Para crear el paciente con email y teléfono hace falta un constructor o *factory* nuevo en `PacienteEntity` (módulo de Juan Cruz): se pide en el PR. *Alternativa descartada*: una segunda entidad JPA sobre la tabla `paciente` dentro de `usuarios/`; duplica el mapeo y se desincroniza.

### D8. Errores en `ProblemDetail`

- `UsuariosExceptionHandler` es un `@RestControllerAdvice(basePackages = "ar.turnos.odontologia.usuarios.api")` con prioridad alta, para no pisar el manejador global que agregue C-06.
- `MethodArgumentNotValidException` → `422 DATOS_INVALIDOS` con la extensión `errores: [{campo, mensaje}]` (Spring responde `400` por defecto; acá se fuerza `422`, regla dura 9).
- Excepción de negocio `RegistroRechazadoException(codigo)` → `409` con `codigo`.
- Los handlers de la cadena de filtros escriben el `ProblemDetail` con el `ObjectMapper` del contexto.
- Ningún mensaje incluye la contraseña, el hash ni datos de otros pacientes; los logs de login fallido registran solo el usuario normalizado.

### D9. Configuración y secretos

`application.yml` (perfil `dev` y base):

```yaml
server.servlet.session:
  timeout: ${SESSION_TIMEOUT:30m}
  cookie: { http-only: true, same-site: lax, secure: ${SESSION_COOKIE_SECURE:false} }
app.seguridad:
  cors-origenes: ${APP_CORS_ORIGENES:http://localhost:5173}
  admin-inicial: { username: ${APP_ADMIN_USERNAME:}, password: ${APP_ADMIN_PASSWORD:} }
```

`.env.example` suma `SESSION_TIMEOUT=30m`, `SESSION_COOKIE_SECURE=false`, `APP_CORS_ORIGENES=http://localhost:5173`, `APP_ADMIN_USERNAME=admin` y `APP_ADMIN_PASSWORD=` (vacío). Ninguna contraseña real en el repo. Los tests usan contraseñas ficticias generadas en el propio test.

CORS: `CorsConfigurationSource` con los orígenes de la lista, `allowCredentials=true`, métodos `GET, POST, PUT, PATCH, DELETE` y encabezados `Content-Type, X-XSRF-TOKEN`. Nunca `*`. Si el frontend pasa a usar el *proxy* de Vite, la lista puede quedar vacía.

### D10. Primer administrador *(a confirmar)*

`CrearAdminInicial` (caso de uso) + `CrearAdminInicialRunner` (`ApplicationRunner`): si `adminInicial.username` y `password` no están vacíos y `UsuarioRepository.existeConRol(ADMIN)` es falso, crea el `ADMIN`. Se prueba con un test unitario y un repositorio en memoria (`src/test`), y la contraseña se valida con la misma `PoliticaContrasena`. *Alternativa descartada*: un `INSERT` con un hash fijo en V3. Deja una credencial conocida en el repo y en producción (regla dura 4).

### D11. Estrategia de tests

| Capa | Herramienta | Qué cubre |
|---|---|---|
| Unitaria (`test`) | JUnit 5 + AssertJ | `NombreUsuario`, `PoliticaContrasena`, `CrearAdminInicial` y `RegistrarPaciente` con repositorios en memoria |
| Integración (`integrationTest`, `@Tag("integration")`) | `@SpringBootTest` + `MockMvc` + `spring-security-test` (`csrf()`), PostgreSQL real | Escenarios de login, CSRF, `401`/`403`, perfil, registro, CORS |
| Integración con servidor real | `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `RestClient` | Atributos de la cookie (`HttpOnly`, `SameSite`, `Secure`), que `MockMvc` no emite |
| Base | JDBC como `RestriccionesTurnoIntegrationTest` | Las `CHECK` y `UNIQUE` de V3 |

- Un test por escenario, con `@DisplayName` igual al nombre del escenario (como en C-01).
- Los escenarios de CSRF **no** usan `.with(csrf())`: piden primero `GET /public/sesion` y reenvían la cookie, para probar el flujo real del frontend.
- Datos ficticios: DNI `9900000x`, emails `@example.com`, usuarios `admin`, `recepcion`, `ana.ejemplo`.
- Cada clase de integración vacía `usuario` y `paciente` en `@BeforeEach` (como C-03).

## Risks / Trade-offs

- **[Choque en `build.gradle` con C-04]** C-04 también agrega `starter-web`. → Avisar a Emanuel y a Sofia; el segundo PR en mergear resuelve el conflicto (son líneas distintas).
- **[Al agregar `starter-security` todo queda protegido]** Cualquier endpoint de otro change que se mergee antes que C-05 deja de responder sin sesión. → Es el comportamiento buscado (denegación por defecto). Se avisa en el PR; C-04 y C-06 escriben sus tests con `@WithMockUser` o sesiones reales.
- **[Receta CSRF para SPA]** Una configuración equivocada deja la cookie sin emitir o rechaza todos los `POST`. → Los escenarios de CSRF se prueban sin el atajo `csrf()` de `spring-security-test` (D11).
- **[Enumeración por el registro]** `DNI_YA_REGISTRADO` y `EMAIL_YA_REGISTRADO` revelan si un DNI o un email existe. → Trade-off aceptado por la claridad para el paciente; el límite de intentos (pregunta abierta) lo mitiga.
- **[Paciente existente sin cuenta]** Con el `409` propuesto, un paciente cargado por la recepción no puede crear su cuenta solo. → Decisión a confirmar; la alternativa (vinculación asistida por la recepción) es otro change.
- **[Test de arranque sin etiqueta]** `TurnosOdontologiaApplicationTest` levanta el contexto completo y necesita la base aunque no tenga `@Tag("integration")`. Es previo a este change; con seguridad sigue arrancando igual. Se informa, no se modifica.
- **[Diferencia con la KB]** `08_arquitectura_propuesta.md` §Seguridad describe al paciente sin cuenta. → Se sigue RN-RO-08 y el scope de C-05; se pide corregir la KB.

## Migration Plan

1. Mergear V3 después de V1 (C-03, ya en `master`). V3 no depende de V2 (C-04), así que el orden entre C-04 y C-05 no importa para Flyway, siempre que ninguno use un número ajeno.
2. Al desplegar, definir `APP_ADMIN_USERNAME` y `APP_ADMIN_PASSWORD` para el primer arranque, iniciar sesión, y después quitar `APP_ADMIN_PASSWORD` del entorno.
3. En producción, `SESSION_COOKIE_SECURE=true`.
4. Rollback: revertir el PR. V3 solo crea una tabla nueva, sin cambios en las existentes; para quitarla, una migración nueva (nunca editar V3 ya mergeada).

## Open Questions

Ninguna cambia lo que se construye en este change si se acepta la propuesta por defecto; si la líder elige otra opción, se actualizan los artefactos antes de implementar:

- Las 7 decisiones *(a confirmar)* de `proposal.md` (usuario = email, política de contraseña, admin inicial, registro sin auto-login, DNI existente → `409`, timeout de 30 min, mismo `401` para todo fallo).
- ¿`profesional_id` también permitido para `ADMIN`? (D6)
- ¿Dónde va el ABM de usuarios del personal? Hoy no está en ningún change de `CHANGES.md`.
- ¿Se agrega en este change un límite de intentos de login (`429`)? Si sí, suma un requisito y sus escenarios.
- ¿El frontend usa CORS (`VITE_API_BASE_URL` a `:8080`) o el *proxy* de Vite? Afecta solo el valor por defecto de `APP_CORS_ORIGENES`; confirmar con Enzo.

## Resolución de decisiones (2026-11-21)

Juan Sebastián (responsable de C-05) aceptó todas las propuestas por defecto. La líder (Sofia) las revisa en el PR; si cambia alguna, se actualizan los artefactos con `openspec-update-change` antes de mergear.

- Las 7 decisiones de `proposal.md`: **aceptadas** tal como están (usuario = email en minúsculas, contraseña de 8 a 72 caracteres, admin inicial desde `APP_ADMIN_USERNAME`/`APP_ADMIN_PASSWORD`, registro sin auto-login, DNI existente → `409 DNI_YA_REGISTRADO`, timeout de 30 min, mismo `401` para todo fallo de login).
- `profesional_id` para `ADMIN` (D6): **permitido**.
- Límite de intentos de login (`429`): **fuera de este change**; queda como mejora futura.
- CORS o *proxy* de Vite: **CORS** con `APP_CORS_ORIGENES` (valor por defecto `http://localhost:5173`); se le avisa a Enzo en el PR.
- ABM de usuarios del personal: **fuera de este change**; se le pide a la líder que lo agregue a `CHANGES.md`.
