# Proposal

## Why

La agenda de C-01 necesita datos que hoy no se pueden cargar: qué profesionales, sillones y prestaciones existen, cuánto dura cada prestación, qué prestaciones hace cada profesional y cuál es el margen entre turnos. C-03 ya creó las tablas base en PostgreSQL. Falta que el administrador pueda gestionarlas, y ese es el prerrequisito de C-06 (API de turnos), C-07 (horarios) y C-10 (reserva online).

## What Changes

- **ABM de profesionales** en `/api/v1/profesionales` (US-001): alta, modificación, consulta y desactivación. La matrícula es única. No se borra: se desactiva (RN-AG-11).
- **ABM de sillones** en `/api/v1/sillones` (US-002): el nombre es único y se desactiva en lugar de borrarse.
- **ABM de prestaciones** en `/api/v1/prestaciones` (US-003): el nombre es único y la duración en minutos es mayor que 0 y múltiplo de 5 (SU-06). Cambiar la duración no modifica los turnos ya creados.
- **Especialidades** con `PUT /api/v1/profesionales/{id}/prestaciones` (RN-AG-15, US-018): define el conjunto de prestaciones que hace cada profesional. Se suma una consulta para saber si un profesional está habilitado para una prestación, que C-06 y C-10 van a usar al crear turnos.
- **Sillón habitual** con `PUT /api/v1/profesionales/{id}/sillon-habitual` (US-018, RN-RO-06): un sillón activo, o ninguno.
- **Margen entre turnos** con `GET` y `PUT /api/v1/configuracion/margen` (RN-AG-04): rechaza valores menores a 10 minutos con `MARGEN_INVALIDO`, reutilizando el objeto de valor `Margen` de C-01.
- **Errores**: `409` con código estable para los conflictos de negocio (`MATRICULA_DUPLICADA`, `NOMBRE_DUPLICADO`) y `422` para las validaciones de entrada (`DURACION_INVALIDA`, `MARGEN_INVALIDO`, `SILLON_INACTIVO`…), en `ProblemDetail` (regla dura 9). La API devuelve DTOs, nunca entidades JPA.
- **Migración V2**: tabla `profesional_prestacion`, tabla `configuracion` con `margen_minutos = 10`, columna `profesional.sillon_habitual_id` y CHECK de duración múltiplo de 5.
- **Seed ficticio solo para el perfil `dev`** (migración aparte, `V2_1`): Dra. Ana Ejemplo y Dr. Bruno Prueba, Sillón 1 y 2, las 4 prestaciones de la KB, especialidades y sillón habitual. No llega a otros entornos.
- **Permisos por rol** (agregado al integrar C-05): según la matriz RBAC de la KB, el personal (`ADMIN`, `RECEPCIONISTA`, `ODONTOLOGO`) consulta y solo `ADMIN` modifica. Otro rol recibe `403 ACCESO_DENEGADO`, como pide el requisito "Permisos por rol en cada operación del personal" de `seguridad-autorizacion`.

**Fuera de alcance**:
- Autenticación, sesión y CSRF: los aporta C-05.
- Rechazar turnos con un profesional, sillón o prestación inactivos (RN-AG-11), o con un profesional no habilitado (RN-AG-15, `PROFESIONAL_NO_HABILITADO`). C-04 provee los datos y la consulta, y la regla se aplica al crear el turno en C-06.
- Horarios de trabajo y ausencias (C-07).
- Pantallas del frontend.

## Capabilities

### New Capabilities
- `catalogo-consultorio`: alta, modificación, consulta y desactivación de profesionales, sillones y prestaciones, con sus reglas de unicidad y duración, más las especialidades y el sillón habitual de cada profesional.
- `configuracion-margen`: consulta y cambio del margen entre turnos del consultorio, con el mínimo de 10 minutos.

### Modified Capabilities
<!-- Ninguna. `agenda-turnos` ya exige el mínimo de 10 minutos para el margen. Este change solo lo hace persistente y configurable por API, sin cambiar sus requisitos. -->

## Impact

- **Código nuevo**: `backend/.../configuracion/` (módulo de Emanuel): dominio, casos de uso, adaptadores JPA y controladores REST.
- **Código ajeno que se toca** (hay que avisar en el PR, según la tabla de dueños de `CHANGES.md`):
  - `backend/build.gradle` (Sofia): se suman `spring-boot-starter-web` y `spring-boot-starter-validation`.
  - `shared/persistence` (Juan Cruz): `ProfesionalEntity`, `SillonEntity` y `PrestacionEntity` necesitan setters o métodos de modificación y la relación con el sillón habitual.
  - `application.yml` (Juan Cruz): en el perfil `dev`, se agrega `db/seed` a las ubicaciones de Flyway.
- **Base de datos**: migración `V2` (número reservado para Emanuel) y `V2_1` de seed, solo en `dev`.
- **API**: nuevos endpoints bajo `/api/v1`, consumidos más adelante por el frontend de configuración (C-09) y por C-06, C-07 y C-10.
- **Riesgo heredado**: C-03 usa ids `BIGINT` y la KB y el dominio de C-01 usan `UUID`. C-04 sigue a la base (`BIGINT`). La diferencia se informa a Sofia y a Juan Cruz para que la resuelvan antes de C-06.
- **Tests**: unitarios del dominio y de los casos de uso, y de integración con PostgreSQL real con `@Tag("integration")`: un test por escenario del spec.
