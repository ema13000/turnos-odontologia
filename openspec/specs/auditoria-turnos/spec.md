# auditoria-turnos Specification

## Purpose
Dejar un registro permanente de las operaciones sobre turnos: qué se hizo, sobre qué turno, quién lo hizo y cuándo (RN-GL-03), para que el consultorio pueda reconstruir la historia de cada turno. En los escenarios, con datos ficticios, el usuario `recepcion` (`RECEPCIONISTA`) opera sobre turnos de "Paciente Uno" (DNI `99000001`) con la Dra. A en el Sillón 1, el 2026-12-01.

## Requirements

### Requirement: Registro de cada operación sobre un turno
El sistema SHALL registrar cada creación, cancelación y reprogramación de un turno con la acción (`CREAR`, `CANCELAR` o `REPROGRAMAR`), el id del turno, el nombre del usuario de la sesión que la hizo y el momento en que ocurrió.

#### Scenario: Se registra la creación
- **GIVEN** el usuario `recepcion` con sesión iniciada
- **WHEN** crea un turno de "Paciente Uno" a las 10:00
- **THEN** existe un registro de auditoría con la acción `CREAR`, el id de ese turno y el actor `recepcion`

#### Scenario: Se registra la cancelación
- **GIVEN** el usuario `recepcion` con sesión iniciada
- **AND** un turno activo de "Paciente Uno" a las 10:00
- **WHEN** lo cancela con el DNI y el número de reserva correctos
- **THEN** existe un registro de auditoría con la acción `CANCELAR`, el id de ese turno y el actor `recepcion`

### Requirement: La reprogramación vincula los dos turnos
Al reprogramar, el sistema SHALL registrar la acción `REPROGRAMAR` sobre el turno original, indicando el id del turno nuevo, y la acción `CREAR` sobre el turno nuevo.

#### Scenario: Se registra la reprogramación
- **GIVEN** el usuario `recepcion` con sesión iniciada
- **AND** un turno activo de "Paciente Uno" a las 10:00
- **WHEN** lo reprograma a las 11:00
- **THEN** existe un registro `REPROGRAMAR` sobre el turno original que indica el id del turno nuevo
- **AND** existe un registro `CREAR` sobre el turno nuevo

### Requirement: Solo se auditan las operaciones que ocurrieron
El registro de auditoría MUST guardarse junto con la operación: si la operación se rechaza, no queda ningún registro de ella.

#### Scenario: Reprogramación rechazada sin registro
- **GIVEN** el usuario `recepcion` con sesión iniciada
- **AND** un turno activo de "Paciente Uno" a las 10:00 y otro turno activo a las 11:00 en el Sillón 1
- **WHEN** reprograma el primero a las 11:10 en el Sillón 1 y la operación se rechaza
- **THEN** no existe ningún registro `REPROGRAMAR` para el turno original

### Requirement: Sin datos sensibles en la auditoría
El registro de auditoría MUST NOT incluir datos de salud, el DNI del paciente ni el número de reserva (RN-NO-01, Ley 25.326).

#### Scenario: La cancelación no guarda el DNI ni el número de reserva
- **GIVEN** el usuario `recepcion` con sesión iniciada
- **AND** un turno activo de "Paciente Uno" a las 10:00
- **WHEN** lo cancela con el DNI `99000001` y el número de reserva del turno
- **THEN** el registro `CANCELAR` no contiene `99000001` ni el número de reserva
