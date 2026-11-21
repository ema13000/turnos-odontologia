# Proposal

## Why

Después de C-01, la regla que impide los turnos superpuestos vive solo en el dominio. Ningún usuario la puede usar y los turnos no se guardan. C-06 la expone en la API del personal y la persiste en PostgreSQL. Así se completa la **doble garantía** contra solapamientos (DD-03): el dominio rechaza con un código claro y la restricción `EXCLUDE` de la base frena las reservas simultáneas (RN-AG-12). Además, al revisar V1 (C-03) aparecieron dos diferencias con el dominio que hay que corregir antes de guardar el primer turno:
- la tabla no admite el estado `RESERVADO`;
- las restricciones `EXCLUDE` bloquean también los turnos `AUSENTE` y `ATENDIDO`.

Es el change C-06 del camino crítico del MVP (`C-01 → C-03 → C-05 → C-06 → C-10`).

## What Changes

- **API del personal para turnos** bajo `/api/v1/turnos`, para los roles `ADMIN`, `RECEPCIONISTA` y `ODONTOLOGO` (US-011, US-012):
  - `POST /turnos`: crea un turno con las reglas de C-01 y responde `201` con el turno y su **número de reserva**;
  - `GET /turnos?desde&hasta[&profesionalId][&sillonId]`: lista los turnos activos del período;
  - `POST /turnos/{id}/cancelar`: cancela el turno después de validar el **DNI** del paciente y el **número de reserva** (RN-AG-07). El horario queda libre de inmediato (RN-AG-08);
  - `POST /turnos/{id}/reprogramar`: con la misma validación, cancela el original y crea el nuevo en **una sola transacción**. Si el nuevo se rechaza, el original no cambia (RN-AG-08).
- **Errores con código estable** en `ProblemDetail`:
  - `409` para los rechazos de negocio (`SOLAPAMIENTO_PROFESIONAL`, `SOLAPAMIENTO_SILLON`, `MARGEN_INSUFICIENTE`, `INICIO_EN_EL_PASADO`, `RECURSO_INACTIVO`, `DATOS_DE_RESERVA_INVALIDOS`, `TURNO_NO_ACTIVO`);
  - `404` para un turno inexistente;
  - `422` para los datos de entrada inválidos.
- **Reservas simultáneas**: la violación de una restricción `EXCLUDE` se traduce a `409 SOLAPAMIENTO_SILLON` o `409 SOLAPAMIENTO_PROFESIONAL`, nunca a un `500`.
- **Recursos inactivos** (RN-AG-11): no se crea ni se reprograma un turno con un profesional, sillón o prestación inactivos.
- **Número de reserva**: 8 caracteres aleatorios y difíciles de adivinar, con formato `XXXX-XXXX` y único por turno. Es lo que identifica al turno ante el paciente, porque los ids BIGINT (DD-12) son correlativos.
- **Auditoría** (RN-GL-03): cada creación, cancelación y reprogramación queda registrada con el usuario de la sesión.
- **Migración V4**:
  - agrega `RESERVADO` al `CHECK` de estado y lo deja como valor por defecto;
  - recrea las dos restricciones `EXCLUDE` para que solo bloqueen los turnos `RESERVADO` y `CONFIRMADO` (RN-AG-17);
  - agrega `codigo_reserva` y la tabla `turno_auditoria`.

  V1 no se edita (regla 3).
- **Un test por escenario** del spec. Los de la API y la base corren contra PostgreSQL real, con `@Tag("integration")`.

**Fuera de alcance** (van en otros changes del roadmap):
- Cancelación y reprogramación online del paciente, con el límite de 24 h (`CANCELACION_FUERA_DE_PLAZO`): C-10 y C-12.
- Horarios de trabajo, bloqueos y ausencias (RN-AG-10, RN-AG-13, RN-AG-16): C-07.
- Sobreturno y extensión (RN-AG-06, RN-AG-14): C-08.
- Especialidades (RN-AG-15) y margen configurable desde la tabla `configuracion`: C-04. Hasta entonces el margen sale de una propiedad con valor 10.
- Notificación al paciente al cancelar o reprogramar: C-11.

## Capabilities

### New Capabilities

- `agenda-api`: contrato HTTP de la agenda para el personal. Cubre los endpoints de turnos, los roles permitidos, el formato de las respuestas, la traducción de los rechazos a `409`/`404`/`422` y la garantía de la base ante reservas simultáneas.
- `auditoria-turnos`: registro de las operaciones sobre turnos, con quién las hizo y cuándo. Los changes posteriores (sobreturno, extensión, confirmación) agregan acciones a esta capacidad.

### Modified Capabilities

- `agenda-turnos`: suma los requisitos de cancelar y reprogramar (RN-AG-07, RN-AG-08), el rechazo por recursos inactivos (RN-AG-11) y el número de reserva de cada turno.

## Impact

- **Código nuevo** (dueña Sofia): `agenda/infrastructure` (adaptadores JPA de `TurnoRepository` y `PrestacionRepository`) y `agenda/api` (controller, DTOs y manejador de errores). Casos de uso `CancelarTurno` y `ReprogramarTurno` en `agenda/application`.
- **Módulo ajeno, con aviso**: `shared/persistence/TurnoEntity` es de Juan Cruz (C-03). Se le agrega `codigoReserva` y el estado inicial pasa a `RESERVADO`. Sus tests de restricciones usan `CONFIRMADO` y `CANCELADO`, y siguen valiendo.
- **Base de datos**: migración **V4**, reservada a C-06 en `CHANGES.md`.
- **Seguridad**: usa la sesión y los roles de C-05 sin cambiar `SecurityConfig`. Cada operación declara sus roles con `@PreAuthorize`.
- **API**: `/api/v1/turnos` deja de responder `404` y pasa a tener endpoints. El frontend de C-09 los consume.
- **Changes que se desbloquean**: C-08, C-09, C-10 y C-11.
