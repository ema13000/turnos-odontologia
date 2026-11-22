# Design

## Context

- **Estado real del esquema**: hoy solo están mergeadas V1, V3 y V4. V1 ya tiene `es_sobreturno`, `motivo_sobreturno VARCHAR(500)` y las dos `EXCLUDE USING gist`; V4 agregó `codigo_reserva`, `estado RESERVADO` y `turno_auditoria` con el `CHECK` `turno_auditoria_accion_valida IN ('CREAR','CANCELAR','REPROGRAMAR')`. **No existe todavía `sillon_habitual_id`**: llega con C-04 / V2, que no está mergeada. No se crea V2 ni se edita V1/V4 (regla dura 3).
- **C-06 dejó costuras reutilizables**: `CodigoRechazo`, `TurnoRechazadoException`, `AgendaExceptionHandler` (`409` con `codigo`, `422 DATOS_INVALIDOS`), `TurnoRepositoryJpa.traducir`, `GeneradorNumeroReserva`, `CrearTurno.preparar`, `RecursosAgenda`, `ServicioAgenda` (transacción + reintento por deadlock), `AuditoriaTurnos`, `TurnosController` y `SecurityConfig`. Dominio y aplicación no importan Spring.
- **Módulos**: `backend/.../agenda/api` es de Sofia; `TurnoEntity` y `shared/persistence` son de Juan Cruz (este change). Tocar los DTOs de `agenda/api` requiere su aprobación explícita en el PR (regla dura 3).
- **Reglas duras aplicables**: dos garantías contra solapamientos juntas (5); sin datos de salud en auditoría, logs ni notificaciones (6); DTOs y errores con código estable (9); sesión con cookie + CSRF, sin JWT (8); `TEXT` + `CHECK`, TIMESTAMPTZ, migración nueva (skill `postgresql-table-design`); `Clock` inyectado, un test por escenario (skill `java-junit`).
- **Motivación y alcance**: ver `proposal.md`. Contrato observable: ver los tres deltas en `specs/`.

## Goals / Non-Goals

**Goals:**

- Cargar sobreturnos sin que el cliente elija sillón, conservando la limpieza obligatoria del sillón y permitiendo solo la excepción del profesional.
- Extender turnos activos con fin absoluto, recalculando `fin_con_margen` con el margen vigente y conservando `numeroReserva`.
- Auditar `SOBRETURNO` y `EXTENDER` sin guardar el motivo en la auditoría, logs ni notificaciones.
- Exponer `esSobreturno` de forma aditiva para C-09 y mantener el comportamiento de `POST /turnos` intacto.

**Non-Goals:**

- No se crea V2, V5 ni V7; no se editan migraciones mergeadas; no se tocan los dos `EXCLUDE` (DD-03).
- No se implementan horarios/ausencias (C-07), frontend (C-09) ni notificaciones (C-11).
- No se reprograma automáticamente el turno siguiente tras una extensión con conflicto: solo se informa el código y el personal decide.
- No se introduce JWT ni se desactiva CSRF. No se guarda el motivo en `turno_auditoria`.

## Decisions

### D1. El cliente nunca envía `sillonId`; el servidor asigna el sillón

`CrearSobreturnoRequest` contiene exactamente `pacienteId`, `profesionalId`, `prestacionId`, `inicio` y `motivo`. No hay campo `sillonId`, ni opcional. El caso de uso `CrearSobreturno` pide al adaptador los sillones activos, descarta los que no cumplen el margen de limpieza para el intervalo `[inicio, fin)` y elige el primero por `id` ascendente. El orden determinista evita que dos pedidos idénticos elijan sillones distintos y hace reproducible el test de carrera.

*Alternativa descartada*: dejar `sillonId` opcional en el pedido. Viola la decisión no negociable de Sofia y abre la puerta a que el personal elija un sillón ocupado.

### D2. Gap C-04 / V2: opción recomendada (a), enganche futuro

`profesional.sillon_habitual_id` **no existe hoy**. Se elige **(a)**: C-08 no depende de C-04; selecciona cualquier sillón activo libre en orden determinista y deja una tarea explícita de enganche para, cuando C-04 aterrice, consultar el sillón habitual del profesional y usarlo si está libre con margen, manteniendo el orden determinista como fallback. La preferencia queda detrás de un método único (`SillonesDisponibles.preferidoPara(profesionalId)`), de modo que el cambio futuro sea local y no modifique el contrato HTTP ni los specs.

*Alternativa (b) descartada por ahora*: declarar dependencia de C-04 y esperar a que V2 esté mergeada. Retrasaría el camino crítico y C-08 no necesita la columna para cumplir RN-AG-06: la preferencia del sillón habitual es una mejora, no una garantía de la regla. **Se eleva a Sofia como pregunta abierta Q1**; si decide (b), se rehace el plan de asignación y las tareas antes del apply. No se crea V2 ni se asume que la columna existe.

### D3. Doble garantía: excepción solo en el dominio del profesional

`Agenda` gana una operación de verificación `verificarSillon(nuevo, existentes)` que reutiliza la misma lógica de margen que `verificar`, pero ignora las colisiones por profesional. `CrearSobreturno` la usa en lugar de `verificar`. Los dos `EXCLUDE` de V1/V4 quedan **intactos**: el de sillón sigue incluyendo sobreturnos y el de profesional sigue con `AND es_sobreturno = FALSE`. Así la base sigue siendo la red de seguridad ante carreras (DD-03, regla dura 5).

*Alternativa descartada*: quitar o relajar el `EXCLUDE` del sillón para sobreturnos. Rompería la limpieza obligatoria (RN-AG-06) y la regla dura 5.

### D4. El código de carrera se traduce solo dentro de la operación de sobreturno

`TurnoRepositoryJpa.traducir` sigue mapeado globalmente a `SOLAPAMIENTO_SILLON`. `CrearSobreturno` (o el borde `ServicioAgenda.crearSobreturno`, según dónde se detecte) captura `TurnoRechazadoException` con `SOLAPAMIENTO_SILLON` o `MARGEN_INSUFICIENTE` y lo reemite como `SIN_SILLON_DISPONIBLE` con detalle en español. `POST /turnos` no cambia: sigue respondiendo `SOLAPAMIENTO_SILLON`. El reintento por deadlock de `ServicioAgenda.INTENTOS = 3` se aplica también al sobreturno, para que la carrera nunca termine en `500`.

*Alternativa descartada*: cambiar `traducir` globalmente. Afectaría a la creación normal y a C-10, que esperan `SOLAPAMIENTO_SILLON`.

```mermaid
flowchart TD
    A[POST /api/v1/turnos/sobreturno<br/>sin sillonId] --> B{¿motivo no vacío?}
    B -- no --> E[422 DATOS_INVALIDOS]
    B -- sí --> C[Calcular fin = inicio + duración]
    C --> D[Buscar sillones activos libres<br/>con margen; orden por id]
    D --> F{¿hay sillón libre?}
    F -- no --> G[409 SIN_SILLON_DISPONIBLE]
    F -- sí --> H[Verificar solo sillón en dominio]
    H --> I[Guardar en transacción:<br/>es_sobreturno=true, motivo_sobreturno]
    I --> J[Auditoría SOBRETURNO<br/>sin motivo]
    I --> K[201 TurnoResponse<br/>con esSobreturno]

    L[POST /api/v1/turnos/{id}/extender<br/>fin absoluto + motivo] --> M{¿turno activo<br/>y no sobreturno?}
    M -- no --> N[409 TURNO_NO_ACTIVO o TURNO_NO_EXTENSIBLE]
    M -- sí --> O[Validar profesional, sillón y margen<br/>como turno nuevo, ignorándose]
    O --> P[fin_con_margen = fin + margen vigente<br/>mismo numeroReserva]
    P --> Q[Auditoría EXTENDER<br/>sin motivo]
    Q --> R[200 TurnoResponse con extendido=true]
```

### D5. Extensión: fin absoluto, validación reutilizada y sin turno nuevo

`ExtenderTurno` recibe `id`, `fin` absoluto y `motivo`. El orden es: turno existente → activo → no sobreturno → `fin > fin actual` → recursos activos y conflictos con margen, reutilizando `CrearTurno.preparar` con el mismo paciente/profesional/sillón/prestación, una sobrescritura opcional del fin calculado y el propio id ignorado como conflicto (`reemplazaA`). Así no se duplica RN-AG-01 a RN-AG-05. La extensión actualiza en la misma fila `fin`, `fin_con_margen = fin + margen vigente`, `extendido = true` y `motivo_extension`, sin cambiar `numeroReserva`, `estado` ni `id`. `TurnoRepositoryJpa.guardar` distingue hoy solo el cambio de estado; se amplía con una operación de actualización de fin que usa el `@Version` existente. El `CHECK` de V6 impide marcar `extendido` sin motivo.

*Alternativa descartada*: crear un turno nuevo y cancelar el original. Cambiaría el `numeroReserva` y generaría registros `CREAR`/`CANCELAR` que no corresponden a la historia real (RN-AG-14).

### D6. Motivo: columnas de dominio, nunca auditoría, logs ni notificaciones

`motivo_sobreturno` ya existe en V1:44. V6 agrega `motivo_extension TEXT` y su `CHECK`. `Turno` pasa a llevar `esSobreturno`, `extendido` y los motivos como campos aditivos; se conservan las fábricas y constructores usados por C-01/C-06 para no reescribir sus tests (estrategia RED→GREEN→TRIANGULATE→REFACTOR). El puerto `AuditoriaTurnos.registrar` sigue recibiendo solo ids, acción, actor y fecha: no hay parámetro donde pueda viajar el motivo. Los DTOs de respuesta no incluyen ningún motivo.

*Alternativa descartada*: guardar el motivo en `turno_auditoria` como `detalle jsonb` (como sugiere la KB). C-06 ya descartó esa columna porque admite datos sensibles; acá además el motivo puede describir la atención (Ley 25.326, regla dura 6).

### D7. API y DTOs: `esSobreturno` aditivo y `extendido` en la respuesta de extensión

`TurnoResponse` gana `esSobreturno` y `extendido`; `TurnoAgendaResponse` gana `esSobreturno`, alimentado por `ConsultaAgenda.TurnoEnAgenda` y `ConsultaAgendaJpa` leyendo la columna ya existente. Son campos booleanos con valor `false` en los turnos normales: aditivos y retrocompatibles, sin migración nueva. `TurnoResponse` se usa en la respuesta de `201` del sobreturno y de `200` de la extensión; `fin_con_margen` permanece interno y se verifica en base. **Estos archivos son del módulo de Sofia**: se pide su aprobación explícita en el PR (regla dura 3, ver Q3).

*Alternativa descartada*: un DTO nuevo solo para sobreturnos. Duplicaría contratos y dejaría a C-09 sin el indicador en la agenda.

### D8. Auditoría: acciones nuevas y CHECK ampliado en V6

`AuditoriaTurnos.Accion` gana `SOBRETURNO` y `EXTENDER`. `ServicioAgenda` registra una acción por operación, en la misma transacción y con reintento; `AuditoriaTurnosJpa` sigue escribiendo solo `turno_id`, `accion`, `actor`, `fecha` y `turno_relacionado_id` (nulo acá). Sin V6, el `CHECK` de V4:49 rechaza la insert y la respuesta sería `500`: por eso la migración es parte del change, no opcional.

### D9. Migración V6

Archivo nuevo `backend/src/main/resources/db/migration/V6__sobreturno_y_extension.sql`:

1. `ALTER TABLE turno ADD COLUMN extendido BOOLEAN NOT NULL DEFAULT FALSE;`
2. `ALTER TABLE turno ADD COLUMN motivo_extension TEXT;`
3. `ADD CONSTRAINT turno_extension_motivo_valido CHECK (extendido = FALSE OR (motivo_extension IS NOT NULL AND length(btrim(motivo_extension)) BETWEEN 1 AND 500));`
4. Alinear la deriva de `knowledge-base/04_modelo_de_datos.md:71-72`: convertir `motivo_sobreturno` a `TEXT` y reemplazar su `CHECK` por uno que exija `length(btrim(...)) BETWEEN 1 AND 500` cuando `es_sobreturno = TRUE`, sin editar V1.
5. `ALTER TABLE turno_auditoria DROP CONSTRAINT turno_auditoria_accion_valida;` y `ADD CONSTRAINT ... CHECK (accion IN ('CREAR','CANCELAR','REPROGRAMAR','SOBRETURNO','EXTENDER'));`

No hay FK ni índices nuevos (no se agrega columna de relación). No se tocan `btree_gist`, `turno_sin_solapamiento_sillon` ni `turno_sin_solapamiento_profesional`. Rollback: no se edita V6; si hace falta revertir, se escribe una migración nueva (regla dura 3).

### D10. Seguridad

No hay cambios en `SecurityConfig`: `/api/v1/**` ya exige `ADMIN`, `RECEPCIONISTA` u `ODONTOLOGO` y `TurnosController` lleva `@PreAuthorize` de clase, así los dos endpoints nuevos heredan la política. La sesión con cookie `HttpOnly` y el CSRF activo siguen vigentes; ningún código nuevo usa JWT ni desactiva CSRF.

### D11. Niveles de test y Strict TDD

Cada escenario GIVEN/WHEN/THEN de los tres deltas tiene un test con `@DisplayName` igual al nombre del escenario. Orden **Strict TDD** por tarea: escribir el test (RED) → mínimo para pasar (GREEN) → casos borde y variantes (TRIANGULATE) → limpiar sin debilitar aserciones (REFACTOR). Nunca se deshabilita ni se borra un test que falla (regla dura 7).

- **Unitarios** (`./gradlew test`, sin I/O, dobles en memoria y `Clock.fixed` 2026-12-01 08:00): dominio y casos de uso. Red de seguridad: los 11 escenarios de `CrearTurnoTest`, 2 de `MargenTest`, `CancelarTurnoTest`, `ReprogramarTurnoTest`, `NumeroReservaTest` y los tests de usuarios, sin cambiar sus aserciones.
- **Integración** (`@Tag("integration")`, PostgreSQL real, `IntegracionAgendaBase`, `Clock` fijo): endpoints, `EXCLUDE`, `fin_con_margen`, `numeroReserva`, auditoría y carrera por el último sillón. Los turnos "ya iniciados" y el sillón ganador de la carrera se siembran por SQL con `JdbcTemplate`. Red de seguridad: `TurnoRepositoryJpaIntegrationTest`, `AgendaConfigIntegrationTest`, `RestriccionesTurnoIntegrationTest`, los tests de C-05 y `TurnosApiIntegrationTest`/`AuditoriaTurnosIntegrationTest` existentes.
- `./gradlew integrationTest` **no** está en `build`/`check`: se ejecuta explícitamente, además de `./gradlew build`.

## Risks / Trade-offs

- **[Riesgo] `sillon_habitual_id` no existe hoy** → se adopta (a) con enganche localizado (D2) y se eleva Q1. Si Sofia elige (b), se revisan specs y tareas antes del apply.
- **[Riesgo] El `CHECK` de V4:49 bloquea `SOBRETURNO` y `EXTENDER` y daría 500** → V6 amplía el `CHECK`; hay un test de integración que inserta cada acción nueva.
- **[Riesgo] Dos sobreturnos simultáneos por el último sillón** → `EXCLUDE` + reintento de `ServicioAgenda` + traducción local; test con dos hilos y `CountDownLatch`, verificando `SIN_SILLON_DISPONIBLE` y ningún `500`.
- **[Riesgo] El profesional puede atender dos turnos a la vez y el dominio debe ignorar solo ese conflicto** → verificación `verificarSillon` dedicada (D3) y test del `EXCLUDE` de profesional con `es_sobreturno = TRUE`.
- **[Riesgo] Extensión con margen vigente distinto al del turno original** → se recalcula desde la configuración actual y se guarda en `fin_con_margen`; test con margen 15.
- **[Riesgo] Tocar `agenda/api`, módulo de Sofia** → aviso antes del PR y aprobación explícita (Q3); los cambios son aditivos y no alteran las respuestas existentes.
- **[Riesgo] El motivo pueda filtrarse a logs o notificaciones** → no entra en `AuditoriaTurnos`, no está en DTOs de respuesta y C-11 queda fuera de scope; grep de verificación en tasks.
- **[Trade-off] Sin preferencia de sillón habitual hasta C-04** → el orden por `id` es predecible y cubre la regla; la preferencia se agrega después sin cambiar el contrato.
- **[Trade-off] `TURNO_NO_EXTENSIBLE` como `409`** → coherente con "errores de negocio = 409" de la regla dura 9; queda como Q2 por si Sofia prefiere `422`.

## Migration Plan

1. Flyway aplica V6 al arrancar; las columnas nuevas no alteran filas existentes (`extendido = FALSE`, `motivo_extension NULL`).
2. Verificar `ddl-auto: validate` contra PostgreSQL con `btree_gist` y comprobar que los dos `EXCLUDE` conservan sus nombres.
3. Desplegar el backend con los dos endpoints; C-09 consume `esSobreturno` cuando su change entre en `master`.
4. Rollback: migración nueva correctiva; nunca editar V6 ya aplicada ni V1/V4.

## Open Questions

- **Q1 (Sofia, prioridad alta)**: ¿confirmamos la opción (a) — C-08 sin dependencia de C-04, asignación determinista ahora y enganche del sillón habitual cuando V2 aterrice— o preferís declarar dependencia de C-04 y esperar? Recomendación: **(a)**.
- **Q2 (Sofia)**: para extender un sobreturno, ¿confirmamos `409 TURNO_NO_EXTENSIBLE` o lo tratamos como `422 DATOS_INVALIDOS`? Recomendación: `409`, por la regla dura 9.
- **Q3 (Sofia)**: aprobación para tocar `agenda/api` (`TurnosController`, `TurnoResponse`, `TurnoAgendaResponse`, `AgendaExceptionHandler` y DTOs nuevos), de su módulo, con cambios aditivos (regla dura 3).
- **Nota de entorno**: las herramientas Engram MCP (`mem_*`) no están expuestas en esta sesión; no se intentó usarlas.
