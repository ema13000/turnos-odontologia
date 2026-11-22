# Proposal

## Why

La agenda actual (C-06) solo crea, cancela y reprograma turnos normales con doble garantía contra solapamientos. El consultorio necesita atender urgencias con **sobreturnos** y registrar **extensiones por complicaciones** (RN-AG-06, RN-AG-14, US-013, US-019) sin romper la limpieza obligatoria del sillón ni la trazabilidad. Hoy `turno_auditoria` solo acepta `CREAR`, `CANCELAR` y `REPROGRAMAR`, de modo que una operación nueva terminaría en un error 500; además la API no expone si un turno es sobreturno, algo que C-09 necesita para la vista de agenda.

## What Changes

- **Nuevo `POST /api/v1/turnos/sobreturno`**: `pacienteId`, `profesionalId`, `prestacionId`, `inicio` y `motivo` (obligatorio). **El cliente nunca envía `sillonId`**: no existe ese campo en el pedido, ni siquiera opcional. El servidor elige un sillón activo libre con margen: prefiere el habitual del profesional cuando la columna exista (ver el gap con C-04 más abajo) y, si no, cualquier sillón activo en orden determinista. Sin sillón libre → `409 SIN_SILLON_DISPONIBLE`. El sobreturno puede pisar la agenda del profesional, **nunca** la limpieza del sillón: los dos `EXCLUDE USING gist` (DD-03) quedan intactos.
- **Nuevo `POST /api/v1/turnos/{id}/extender`**: recibe un `fin` **absoluto** y un `motivo` administrativo obligatorio. Solo extiende turnos `RESERVADO`/`CONFIRMADO`, nunca un sobreturno; recalcula `fin_con_margen = fin + margen vigente` y valida profesional, sillón y margen como un turno nuevo. Conserva el `numeroReserva`. En caso de colisión responde `409` con código estable: el personal decide después si reprograma el turno siguiente o registra una extensión como sobreturno (no se reprograma nada automáticamente).
- **Traducción local del código de carrera**: dentro de la operación de sobreturno, `SOLAPAMIENTO_SILLON` se traduce a `SIN_SILLON_DISPONIBLE`; `TurnoRepositoryJpa.traducir` y `POST /turnos` siguen devolviendo `SOLAPAMIENTO_SILLON`.
- **Migración V6** (archivo nuevo): `extendido BOOLEAN NOT NULL DEFAULT FALSE`, `motivo_extension TEXT` con `CHECK` y ampliación del `CHECK` de `turno_auditoria.accion` con `SOBRETURNO` y `EXTENDER`. No se editan V1 ni V4; no se tocan los dos `EXCLUDE`.
- **Visibilidad para C-09**: `esSobreturno: boolean` aditivo en `TurnoAgendaResponse` (vía `ConsultaAgenda.TurnoEnAgenda`) y en `TurnoResponse`; sin migration nueva (`es_sobreturno` ya existe en V1). Estos archivos son del módulo de Sofia → se requiere su aprobación explícita en el PR (regla dura 3).
- **Motivo = dato administrativo, nunca clínico**: se guarda en `turno.motivo_sobreturno` (V1:44) y en la nueva columna `motivo_extension`. NUNCA en `turno_auditoria`, logs ni notificaciones (regla dura 6, Ley 25.326). Se alinea la deriva documental de `knowledge-base/04_modelo_de_datos.md:71-72` (`varchar(255)` vs V1 `VARCHAR(500)`) usando `TEXT` + `CHECK` en V6, sin editar V1.
- **Gap C-04 / V2 (documentado en `design.md`)**: hoy solo existen V1, V3 y V4; `sillon_habitual_id` llega con C-04 (V2, aún no mergeada). Se recomienda la opción (a): C-08 elige cualquier sillón activo libre en orden determinista ahora, y deja una tarea de enganche para preferir el sillón habitual cuando C-04 aterrice. No se crea V2 ni se asume que la columna existe. Se eleva como pregunta abierta a Sofia.
- Sin cambios de seguridad: sesión con cookie y CSRF activo; personal (`ADMIN`, `RECEPCIONISTA`, `ODONTOLOGO`), paciente `403`. Sin JWT.

## Capabilities

### New Capabilities

- (ninguna: la conducta nueva se expresa como deltas de capacidades existentes)

### Modified Capabilities

- `agenda-turnos`: reglas de sobreturno (motivo, excepción solo del profesional, sillón libre con margen, asignación determinista) y de extensión (fin absoluto, solo turnos activos no sobreturno, `fin_con_margen` recalculado, `numeroReserva` inmutable).
- `agenda-api`: dos endpoints nuevos, DTOs sin `sillonId` en sobreturno, `esSobreturno` aditivo en las respuestas, y códigos `409` estables (`SIN_SILLON_DISPONIBLE`, `SOLAPAMIENTO_PROFESIONAL`, `SOLAPAMIENTO_SILLON`, `MARGEN_INSUFICIENTE`, `TURNO_NO_EXTENSIBLE`).
- `auditoria-turnos`: acciones `SOBRETURNO` y `EXTENDER` con actor y fecha, y prohibición de guardar el motivo en la auditoría.

## Impact

- **Dominio/aplicación** (`agenda/domain`, `agenda/application`): `CodigoRechazo`, `Turno`, `Agenda`, `CrearTurno.preparar`, casos de uso nuevos, puertos (`RecursosAgenda`, `TurnoRepository`, `GeneradorNumeroReserva`, `AuditoriaTurnos`).
- **Infraestructura** (`agenda/infrastructure`): `ServicioAgenda` (transacción + reintento), `TurnoRepositoryJpa` (traducción localizada), `AuditoriaTurnosJpa`, `TurnoEntity`, `AgendaConfig`.
- **API** (`agenda/api`): `TurnosController`, DTOs nuevos y existentes, `AgendaExceptionHandler` (módulo de Sofia: requiere aprobación en el PR).
- **Base**: migración nueva `V6__sobreturno_y_extension.sql`; `btree_gist` y los dos `EXCLUDE` sin cambios.
- **Tests**: unitarios por escenario (`Strict TDD`: RED→GREEN→TRIANGULATE→REFACTOR) e integración `@Tag("integration")` con PostgreSQL real y `Clock` fijo 2026-12-01 08:00. `integrationTest` **no** está en `build`/`check`: se ejecuta explícitamente.
- **Fuera de scope**: C-07 (horarios/ausencias), C-09 (frontend), C-11 (notificaciones), V2/V5/V7, editar migraciones mergeadas, reprogramación automática del turno siguiente.
