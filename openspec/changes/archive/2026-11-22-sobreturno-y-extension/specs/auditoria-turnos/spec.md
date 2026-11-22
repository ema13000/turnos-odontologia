# Spec Delta

## ADDED Requirements

### Requirement: Registro de sobreturnos y extensiones

El sistema SHALL registrar cada sobreturno con la acción `SOBRETURNO` y cada extensión con la acción `EXTENDER`, sobre el id del turno, con el `username` del actor y la fecha del momento. El registro SHALL guardarse en la misma transacción que la operación: si se rechaza, no queda ningún registro. La extensión MUST NOT crear un turno nuevo ni un registro `CREAR`.

#### Scenario: Se registra el sobreturno
- **GIVEN** el usuario `recepcion` con sesión iniciada
- **WHEN** crea un sobreturno de "Paciente Uno" a las 10:00 con un sillón libre
- **THEN** existe un registro de auditoría con la acción `SOBRETURNO`, el id de ese turno y el actor `recepcion`
- **AND** no existe un registro `CREAR` para ese turno

#### Scenario: Se registra la extensión
- **GIVEN** el usuario `ana.ejemplo` con sesión iniciada
- **AND** un turno activo de "Paciente Uno" de 10:00 a 10:30
- **WHEN** extiende el turno hasta las 10:45 con un motivo administrativo
- **THEN** existe un registro de auditoría con la acción `EXTENDER`, el id de ese turno y el actor `ana.ejemplo`
- **AND** no se crea un turno nuevo

#### Scenario: Sobreturno rechazado sin registro
- **GIVEN** turnos activos que ocupan los dos sillones de 10:00 a 10:30
- **WHEN** el usuario `recepcion` intenta un sobreturno a las 10:10 y la operación se rechaza
- **THEN** no existe ningún registro `SOBRETURNO` para esa operación

#### Scenario: Extensión rechazada sin registro
- **GIVEN** un turno activo de 10:00 a 10:30 y otro de 11:00 en el mismo sillón
- **WHEN** el usuario `recepcion` intenta extender el primero hasta las 11:00 y la operación se rechaza
- **THEN** no existe ningún registro `EXTENDER` para ese turno
- **AND** el turno sigue terminando a las 10:30

### Requirement: Sin motivo en la auditoría ni en canales externos

El registro de auditoría MUST NOT incluir el motivo del sobreturno ni de la extensión, porque es un dato administrativo que puede describir la atención. La tabla SHALL guardar solo ids, acción, actor y fecha, sin columnas de texto libre. El motivo MUST NOT aparecer en logs ni en notificaciones (RN-NO-01, Ley 25.326).

#### Scenario: El motivo del sobreturno no se audita
- **GIVEN** un sobreturno creado con un motivo administrativo
- **WHEN** se consulta el registro `SOBRETURNO` de ese turno en la tabla de auditoría
- **THEN** ninguna columna del registro contiene el texto del motivo

#### Scenario: El motivo de la extensión no se audita
- **GIVEN** un turno extendido con un motivo administrativo
- **WHEN** se consulta el registro `EXTENDER` de ese turno en la tabla de auditoría
- **THEN** ninguna columna del registro contiene el texto del motivo
