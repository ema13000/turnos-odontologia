# Design

## Context

- C-03 dejó en `master` las tablas `profesional`, `sillon`, `prestacion`, `paciente` y `turno` (`V1__base_agenda.sql`), con ids `BIGINT GENERATED ALWAYS AS IDENTITY`, y entidades JPA mínimas en `shared/persistence`, sin setters. `ddl-auto: validate`, así que toda columna nueva necesita su migración.
- El build no tiene todavía `spring-boot-starter-web` ni `spring-boot-starter-validation`. Este es el primer change con endpoints REST.
- C-01 ya tiene `Margen` (en `agenda/domain`), que rechaza valores menores a 10 con `TurnoRechazadoException(MARGEN_INVALIDO)`. Su design (D5) anticipa que C-04 provee el margen vigente a `CrearTurno`.
- Los tests de integración de C-03 usan PostgreSQL real con `@Tag("integration")`, corren con `./gradlew integrationTest` y vacían las tablas con `TRUNCATE ... CASCADE` antes de cada test.
- C-05 (seguridad) corre en paralelo y todavía no está en `master`.

## Goals / Non-Goals

**Goals:**
- ABM de profesionales, sillones y prestaciones, especialidades, sillón habitual y margen, con un test por escenario del spec.
- Un margen vigente y una consulta de habilitación que C-06 y C-10 puedan usar sin depender de la API.
- Tocar lo mínimo de los módulos ajenos y dejarlo explícito para sus dueños.

**Non-Goals:**
- Seguridad de los endpoints (C-05).
- Aplicar RN-AG-11 y RN-AG-15 al crear turnos (C-06).
- Paginación, búsqueda y ordenamiento de los listados: un consultorio chico tiene pocos registros.

## Decisions

### D1. Módulo `configuracion` con puertos y adaptadores

```
backend/src/main/java/ar/turnos/odontologia/configuracion/
  domain/          DuracionPrestacion, CodigoError, ConfiguracionException
  application/     casos de uso por recurso (ProfesionalService, SillonService,
                   PrestacionService, EspecialidadesService, MargenService),
                   puertos MargenVigente y HabilitacionProfesional
  infrastructure/  repositorios Spring Data y adaptadores JPA de los puertos
  api/             controladores REST, DTOs (records) y ConfiguracionExceptionHandler
```

Es la carpeta asignada a Emanuel en la tabla de dueños. Los DTOs son `record` con Bean Validation. La API nunca devuelve entidades (regla dura 9).

*Alternativa descartada*: controladores que usan directamente los repositorios. Es menos código, pero deja las reglas (unicidad, duración, especialidades) repartidas en la capa web y sin tests de unidad.

### D2. Reutilizar las entidades de `shared/persistence`

Las tablas son de C-03, y dos entidades JPA sobre la misma tabla causarían inconsistencias en la caché de Hibernate. Por eso C-04 extiende las entidades existentes:
- `ProfesionalEntity`, `SillonEntity` y `PrestacionEntity`: métodos de modificación (`actualizar(...)`, `activar()`, `desactivar()`).
- `ProfesionalEntity`: `@ManyToOne` opcional a `SillonEntity` (`sillon_habitual_id`) y `@ManyToMany` a `PrestacionEntity` sobre `profesional_prestacion`.

Los repositorios Spring Data viven en `configuracion/infrastructure`. **Hay que avisar a Juan Cruz en el PR**, porque `shared/persistence` es suyo (regla dura 3).

### D3. Ids `BIGINT`

La API y los DTOs usan `Long`, como la base de C-03. La KB y el dominio de C-01 usan `UUID`. No se resuelve acá: se informa a Sofia y a Juan Cruz (ver Riesgos).

### D4. Errores con `ProblemDetail` y código estable

- `ConfiguracionException(CodigoError, mensaje)`. Cada código tiene su status:

  | Código | Status |
  |---|---|
  | `MATRICULA_DUPLICADA`, `NOMBRE_DUPLICADO` | 409 |
  | `DURACION_INVALIDA`, `PRESTACION_INACTIVA`, `SILLON_INACTIVO`, `MARGEN_INVALIDO`, `DATOS_INVALIDOS` | 422 |
  | `RECURSO_NO_ENCONTRADO` | 404 |

- `ConfiguracionExceptionHandler` es un `@RestControllerAdvice(basePackages = "ar.turnos.odontologia.configuracion")`: solo atiende a este módulo, así no choca con el manejador global que agregue C-06. Devuelve `ProblemDetail` con la propiedad `codigo` y, para `DATOS_INVALIDOS`, la lista `errores` con campo y mensaje.
- `MethodArgumentNotValidException` (Bean Validation) se traduce a `422 DATOS_INVALIDOS`.
- `TurnoRechazadoException` con `MARGEN_INVALIDO`, lanzada por `Margen`, se traduce a `422 MARGEN_INVALIDO`.

### D5. Unicidad: chequeo previo y restricción de la base

El servicio verifica la matrícula o el nombre antes de guardar, para dar el código correcto. Si dos altas compiten, la restricción `UNIQUE` de V1 lanza `DataIntegrityViolationException`, y el manejador la traduce al mismo `409` según el nombre de la restricción. El chequeo previo da el mensaje claro y la base es la garantía.

### D6. Duración múltiplo de 5 en el dominio y en la base

`DuracionPrestacion.deMinutos(int)` rechaza valores ≤ 0 o no múltiplos de 5 con `DURACION_INVALIDA`. V2 agrega `CHECK (duracion_minutos % 5 = 0)` como segunda garantía. El seed de C-03 (Consulta, 30 min) lo cumple.

La duración no se copia a los turnos: `turno.fin` ya se guarda al crearlo (C-01 y C-03), así que cambiar la prestación no toca los turnos existentes. El escenario lo verifica con un test de integración.

### D7. Margen en la tabla `configuracion` y puerto `MargenVigente`

- Tabla `configuracion(clave VARCHAR(50) PK, valor VARCHAR(100) NOT NULL)`, como dice la KB. V2 inserta `('margen_minutos', '10')`, así el valor predeterminado existe en todos los entornos.
- `MargenService.cambiar(minutos)` construye `Margen.deMinutos(minutos)`: la regla del mínimo vive en un solo lugar (C-01) y se guarda solo si es válido.
- Puerto `MargenVigente.actual(): Margen`, implementado en `configuracion/infrastructure`, lee siempre de la base, sin caché, porque se usa poco y así un cambio aplica de inmediato. C-06 lo inyecta para construir `CrearTurno`.
- `GET /api/v1/configuracion/margen` devuelve `{ "minutos": 10 }`. `PUT` recibe el mismo cuerpo.

### D8. Especialidades y habilitación

- `PUT /api/v1/profesionales/{id}/prestaciones` con `{ "prestacionIds": [..] }` reemplaza el conjunto completo dentro de una transacción. Si alguna prestación está inactiva, lanza `PRESTACION_INACTIVA` y no se cambia nada. Si alguna no existe, lanza `RECURSO_NO_ENCONTRADO`.
- `GET /api/v1/profesionales/{id}/prestaciones` lista las especialidades.
- Puerto `HabilitacionProfesional.estaHabilitado(profesionalId, prestacionId): boolean`, para C-06 y C-10. El escenario "Profesional no habilitado" se prueba contra este puerto.

### D9. Endpoints

| Recurso | Endpoints |
|---|---|
| Profesionales | `GET /profesionales`, `GET /profesionales/{id}`, `POST /profesionales`, `PUT /profesionales/{id}`, `POST /profesionales/{id}/desactivar`, `POST /profesionales/{id}/activar` |
| Sillones | igual, bajo `/sillones` |
| Prestaciones | igual, bajo `/prestaciones` |
| Especialidades | `GET` y `PUT /profesionales/{id}/prestaciones` |
| Sillón habitual | `PUT /profesionales/{id}/sillon-habitual` con `{ "sillonId": 1 }` o `{ "sillonId": null }` |
| Margen | `GET` y `PUT /configuracion/margen` |

Todo va bajo `/api/v1`. No hay `DELETE` (RN-AG-11). Las altas devuelven `201` con `Location`, y los listados devuelven todos los registros con su campo `activo`.

*Alternativa descartada*: `PATCH` con `{ "activo": false }`. Es más genérico, pero mezcla la desactivación con la edición de datos y complica la validación.

### D10. Migraciones `V2` y seed de desarrollo `V2_1`

- `db/migration/V2__configuracion_consultorio.sql`: `profesional_prestacion (profesional_id, prestacion_id)` con PK compuesta y FK a las dos tablas; `configuracion` con la fila del margen; `profesional.sillon_habitual_id BIGINT NULL REFERENCES sillon(id)`; y el CHECK de duración.
- `db/seed/V2_1__seed_dev.sql`: los datos ficticios de la KB (profesionales, sillones, 4 prestaciones, especialidades con conducto solo para el Dr. Bruno Prueba, y sillón habitual). El perfil `dev` de `application.yml` agrega `spring.flyway.locations: classpath:db/migration,classpath:db/seed`. Otros perfiles no ven esa carpeta.
- `V2_1` es menor que `V3`, así que no se mezcla con las migraciones de los demás changes. Los usuarios del seed (`admin`, `recepcion`) los agrega C-05.

### D11. Tests

- **Unitarios**: `DuracionPrestacion` y los servicios, con dobles en memoria de los puertos, sin base.
- **Integración** (`@Tag("integration")`, `@SpringBootTest` con `MockMvc`, PostgreSQL real): un test por escenario del spec, entrando por la API, con el `@DisplayName` igual al nombre del escenario. Usan el perfil `test`, que no carga el seed, y vacían las tablas antes de cada test, como C-03.
- El escenario "Margen después de reiniciar" se prueba leyendo el valor con un contexto nuevo (o directamente por JDBC) después de cambiarlo por la API.

### D12. Permisos por rol con `@PreAuthorize` (agregado al integrar C-05)

- C-05 deja `/api/v1/**` abierto a los tres roles del personal y pide que cada operación declare sus roles. C-04 anota con `@PreAuthorize("hasRole('ADMIN')")` todas las operaciones que modifican (`POST` y `PUT`). Los `GET` quedan para todo el personal, según la matriz RBAC de la KB.
- No se toca `SecurityConfig` de C-05: la seguridad por método ya está habilitada (`@EnableMethodSecurity`), y el `403` con `ACCESO_DENEGADO` lo responde el manejador de C-05.
- Los tests de integración entran como `admin` con `@WithMockUser`, y un test puede pedir otro rol con su propio `@WithMockUser`. El CSRF se manda como el frontend (la cookie `XSRF-TOKEN` y el encabezado `X-XSRF-TOKEN` con el mismo valor). *Alternativa descartada*: el atajo `csrf()` de spring-security-test, porque reemplaza el repositorio CSRF del filtro compartido y rompe los tests de C-05 que corren después en el mismo contexto.

### D13. Migraciones fuera de orden en `dev` y `test` (agregado al integrar C-05)

Los números de migración están reservados por change, pero los PR se mergean en otro orden: C-05 (V3) entró antes que C-04 (V2). Una base de desarrollo con V3 aplicada rechaza V2 (`Detected resolved migration not applied to database: 2`). Por eso los perfiles `dev` y `test` usan `spring.flyway.out-of-order: true`. Lo verifiqué con una base que solo tenía V1 y V3: sin el cambio la app no arranca, y con el cambio aplica V2 y V2_1 después de V3.

## Risks / Trade-offs

- **[Riesgo] Ids `BIGINT` contra `UUID`.** La KB y el dominio de C-01 (`Turno`, `CrearTurnoCommand`) usan `UUID`, y la base de C-03 usa `BIGINT`. C-06 va a tener que conectar los dos. → Mitigación: C-04 sigue a la base y el PR lo informa a Sofia y a Juan Cruz para que decidan antes de C-06.
- **[Riesgo] Estados de turno.** El CHECK de V1 no incluye `RESERVADO`, que el dominio de C-01 usa como estado inicial. No afecta a C-04 (los tests insertan turnos `CONFIRMADO`), pero se informa junto con el riesgo anterior.
- **[Riesgo resuelto] Endpoints sin autenticación hasta C-05.** C-05 ya está en `master`, y C-04 restringe las escrituras a `ADMIN` (D12).
- **[Riesgo resuelto] Los tests de integración borraban la base local de desarrollo** (los de C-03 y C-05 hacían `TRUNCATE` sobre la base de `dev`). Ahora todos usan el perfil `test` y `DB_TEST_URL`.
- **[Riesgo] Zona horaria en Windows.** Java toma la hora de Argentina como `America/Buenos_Aires`, que PostgreSQL 16 rechaza. `build.gradle` la fija en los tests y en `bootRun`. Para el jar (C-14) hay que pasar `-Duser.timezone=America/Argentina/Buenos_Aires`.
- **[Trade-off] Cambios en `shared/persistence` y `build.gradle`**, que son de otros dueños. Son aditivos (métodos y relaciones nuevas, dos starters) y se avisan en el PR.
- **[Trade-off] Sin caché del margen.** Se hace una consulta por cada turno creado, lo que es despreciable para un consultorio chico.
