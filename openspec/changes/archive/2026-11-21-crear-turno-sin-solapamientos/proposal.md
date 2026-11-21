# Proposal

## Why

En un consultorio chico, los turnos se superponen: dos pacientes con el mismo profesional, o dos atenciones en el mismo sillón sin tiempo para limpiarlo. Ninguno de los 16 sistemas relevados en el Discovery documenta cómo lo previene (informe, C.3; NE-01). Este change construye ese núcleo, que el resto del roadmap usa (DD-02): la regla que decide si un turno nuevo entra en la agenda. Es el change C-01 y el ciclo OPSX que exige la Etapa 6 de la consigna.

## What Changes

- **Scaffolding del backend** (`backend/`): proyecto Gradle con wrapper, Java 21, Spring Boot 3.x, JUnit 5 y AssertJ, con `./gradlew test` en verde. Se mergea temprano a `master` para desbloquear C-03.
- **Dominio de agenda** puro, sin base de datos ni framework web, que aplica:
  - el cálculo del fin del turno a partir de la duración de la prestación (RN-AG-01);
  - la prohibición de turnos activos en conflicto por **profesional** (RN-AG-02) y por **sillón** (RN-AG-03);
  - el **margen** entre turnos que comparten recurso, configurable con un mínimo de 10 minutos (RN-AG-04, RN-AG-05);
  - el rechazo de turnos con inicio en el pasado (RN-AG-09).
- **Caso de uso Crear turno**, que obtiene la duración de la prestación y los turnos activos a través de puertos, con implementaciones en memoria para los tests.
- **Códigos de error estables** para cada rechazo: `SOLAPAMIENTO_PROFESIONAL`, `SOLAPAMIENTO_SILLON`, `MARGEN_INSUFICIENTE`, `INICIO_EN_EL_PASADO` y `MARGEN_INVALIDO`. Si hay más de un conflicto, se informa uno solo, con la prioridad profesional > sillón > margen.
- **Un test por escenario** del spec.

**Fuera de alcance** (van en otros changes del roadmap):
- Persistencia en PostgreSQL y la restricción de exclusión que garantiza la atomicidad ante reservas simultáneas (RN-AG-12): C-03 y C-06.
- API REST y traducción de los errores a `409`/`422`: C-06.
- Horarios de trabajo, bloqueos y ausencias (RN-AG-10, RN-AG-13, RN-AG-16): C-07.
- Sobreturno y extensión (RN-AG-06, RN-AG-14): C-08.
- Recursos inactivos y especialidades (RN-AG-11, RN-AG-15): C-04 y C-07.
- Cancelar y reprogramar (RN-AG-07, RN-AG-08): C-06.

## Capabilities

### New Capabilities

- `agenda-turnos`: reglas para aceptar o rechazar un turno en la agenda del consultorio. Cubre el cálculo de su intervalo, el conflicto por profesional y por sillón con margen y la validación temporal. Los changes de agenda posteriores (cancelar, reprogramar, sobreturno, horarios) agregan sus requisitos a esta capacidad.

### Modified Capabilities

Ninguna: es la primera capacidad del proyecto.

## Impact

- **Código nuevo**: `backend/` (build Gradle) y los paquetes `ar.turnos.odontologia.agenda.domain` y `ar.turnos.odontologia.agenda.application`, cuyo dueño es Sofia (`CHANGES.md`).
- **Dependencias**: Spring Boot 3.x (solo el build y el test starter en este change), JUnit 5 y AssertJ. Sin base de datos ni Docker: los tests de este change corren en cualquier máquina con Java 21.
- **Changes que se desbloquean**: C-03 (con el scaffolding), C-06 y C-07 (con el dominio).
- **Sin migraciones**: este change no reserva números de Flyway.
