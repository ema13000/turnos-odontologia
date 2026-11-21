# Spec Delta

## ADDED Requirements

### Requirement: Crear un sobreturno por la API

`POST /api/v1/turnos/sobreturno` con `pacienteId`, `profesionalId`, `prestacionId`, `inicio` y `motivo` SHALL crear el sobreturno con las reglas de `agenda-turnos` y responder `201` con el id, `sillonId` asignado, `inicio`, `fin`, `estado`, `numeroReserva` y `esSobreturno`. El contrato MUST NOT incluir el campo `sillonId`: el cliente nunca elige el sillón. Las fechas MUST viajar en ISO-8601 con zona horaria.

#### Scenario: Recepción crea un sobreturno
- **GIVEN** el usuario `recepcion` con sesión iniciada y los dos sillones activos y libres
- **WHEN** pide `POST /api/v1/turnos/sobreturno` para "Paciente Uno" con la Dra. A, "Consulta", inicio `2026-12-01T10:00:00-03:00` y un motivo administrativo
- **THEN** la respuesta es `201` con `estado` `RESERVADO`, `fin` `2026-12-01T10:30:00-03:00`, `esSobreturno` verdadero y un `numeroReserva`
- **AND** el cuerpo incluye el `sillonId` asignado por el servidor

#### Scenario: El pedido no declara sillonId
- **GIVEN** el usuario `recepcion` con sesión iniciada
- **WHEN** revisa el contrato del pedido de sobreturno
- **THEN** no existe el campo `sillonId`, ni siquiera como campo opcional
- **AND** el sillón se asigna en el servidor según las reglas de `agenda-turnos`

### Requirement: Motivo obligatorio del sobreturno

El sistema MUST responder `422` con `DATOS_INVALIDOS` y el campo `motivo` en la lista de errores cuando el motivo falta, está vacío, son solo espacios o supera los 500 caracteres. El motivo SHALL ser administrativo y la respuesta MUST NOT incluirlo.

#### Scenario: Sobreturno sin motivo
- **GIVEN** el usuario `recepcion` con sesión iniciada
- **WHEN** pide `POST /api/v1/turnos/sobreturno` sin `motivo` o con `"   "`
- **THEN** la respuesta es `422` con el código `DATOS_INVALIDOS`
- **AND** la lista de errores incluye el campo `motivo`

#### Scenario: La respuesta no expone el motivo
- **GIVEN** un sobreturno creado con un motivo administrativo
- **WHEN** el personal consulta `GET /api/v1/turnos` o recibe la respuesta de creación
- **THEN** ninguno de los cuerpos incluye el motivo

### Requirement: Extender un turno por la API

`POST /api/v1/turnos/{id}/extender` con `fin` absoluto y `motivo` SHALL extender un turno `RESERVADO` o `CONFIRMADO` y responder `200` con el turno actualizado, `extendido` verdadero y el mismo `numeroReserva`. Un `fin` sin zona horaria o un motivo inválido MUST responder `422 DATOS_INVALIDOS`. Un id inexistente MUST responder `404 TURNO_INEXISTENTE`. El sistema MUST NOT reprogramar automáticamente el turno siguiente.

#### Scenario: Recepción extiende un turno
- **GIVEN** un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
- **WHEN** pide `POST /api/v1/turnos/{id}/extender` con `fin` `2026-12-01T10:45:00-03:00` y un motivo administrativo
- **THEN** la respuesta es `200` con `fin` `2026-12-01T10:45:00-03:00`, `extendido` verdadero y el mismo `numeroReserva`

#### Scenario: Extensión con fin sin zona horaria
- **GIVEN** el usuario `recepcion` con sesión iniciada
- **WHEN** pide extender un turno con `fin` `2026-12-01T10:45:00`
- **THEN** la respuesta es `422` con el código `DATOS_INVALIDOS` y el campo `fin`

#### Scenario: Extensión de un turno inexistente
- **GIVEN** el usuario `recepcion` con sesión iniciada
- **WHEN** pide `POST /api/v1/turnos/999999/extender` con un `fin` y un motivo válidos
- **THEN** la respuesta es `404` con el código `TURNO_INEXISTENTE`

#### Scenario: Extensión rechazada no reprograma el siguiente
- **GIVEN** un turno activo de 10:00 a 10:30 y otro de 11:00 a 11:30 en el mismo sillón
- **WHEN** pide extender el primero hasta las 11:00 y la operación se rechaza
- **THEN** la respuesta es `409` con el código `SOLAPAMIENTO_SILLON`
- **AND** el turno siguiente sigue en su horario original

### Requirement: Códigos estables de rechazo de sobreturnos y extensiones

Los rechazos de negocio de estos endpoints MUST responder `409` con un `ProblemDetail` que incluya la propiedad `codigo`: `SIN_SILLON_DISPONIBLE`, `SOLAPAMIENTO_SILLON`, `MARGEN_INSUFICIENTE`, `TURNO_NO_ACTIVO` o `TURNO_NO_EXTENSIBLE`, con detalle legible en español. La traducción de `SOLAPAMIENTO_SILLON` a `SIN_SILLON_DISPONIBLE` SHALL aplicarse solo a la operación de sobreturno.

#### Scenario: Sobreturno sin sillón libre
- **GIVEN** turnos activos que ocupan los dos sillones de 10:00 a 10:30
- **WHEN** el usuario `recepcion` pide un sobreturno de "Consulta" a las 10:10
- **THEN** la respuesta es `409` con el código `SIN_SILLON_DISPONIBLE`

#### Scenario: Extensión de un sobreturno
- **GIVEN** un sobreturno activo de 10:00 a 10:30
- **WHEN** el usuario `recepcion` pide extenderlo hasta las 10:45
- **THEN** la respuesta es `409` con el código `TURNO_NO_EXTENSIBLE`

#### Scenario: Un turno normal mantiene su código de sillón
- **GIVEN** un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
- **WHEN** el usuario `recepcion` pide `POST /api/v1/turnos` con el Dr. B en el Sillón 1 a las 10:15
- **THEN** la respuesta es `409` con el código `SOLAPAMIENTO_SILLON`, no `SIN_SILLON_DISPONIBLE`

### Requirement: Indicador de sobreturno en las respuestas

Las respuestas `TurnoResponse` y `TurnoAgendaResponse` SHALL incluir `esSobreturno: boolean`, de forma aditiva y retrocompatible. El indicador MUST reflejar la columna `es_sobreturno` ya existente, sin requerir una migración nueva. Las respuestas MUST NOT incluir el motivo ni datos de salud.

#### Scenario: La agenda distingue los sobreturnos
- **GIVEN** un turno normal y un sobreturno activos el 2026-12-01
- **WHEN** el personal pide `GET /api/v1/turnos` para ese día
- **THEN** ambos turnos aparecen
- **AND** solo el sobreturno tiene `esSobreturno` verdadero

#### Scenario: Respuesta de creación sin datos sensibles
- **GIVEN** un sobreturno creado con un motivo administrativo
- **WHEN** llega la respuesta `201` de `POST /api/v1/turnos/sobreturno`
- **THEN** el cuerpo no incluye `motivo`, `motivoSobreturno` ni datos de salud

### Requirement: Roles para sobreturno y extensión

Las operaciones nuevas SHALL estar permitidas a `ADMIN`, `RECEPCIONISTA` y `ODONTOLOGO`. Una sesión `PACIENTE` MUST recibir `403` con el código `ACCESO_DENEGADO`, sin crear ni modificar ningún turno. Se mantiene la sesión con cookie `HttpOnly` y CSRF activo; no se usa JWT.

#### Scenario: Un odontólogo crea un sobreturno
- **GIVEN** el usuario `ana.ejemplo` con sesión iniciada
- **WHEN** pide `POST /api/v1/turnos/sobreturno` con datos válidos y un sillón libre
- **THEN** la respuesta es `201`

#### Scenario: Un paciente intenta crear un sobreturno
- **GIVEN** "Paciente Uno" con sesión iniciada
- **WHEN** pide `POST /api/v1/turnos/sobreturno` con datos válidos
- **THEN** la respuesta es `403` con el código `ACCESO_DENEGADO`
- **AND** no se crea ningún turno

#### Scenario: Un paciente intenta extender un turno
- **GIVEN** "Paciente Uno" con sesión iniciada
- **WHEN** pide `POST /api/v1/turnos/{id}/extender` con datos válidos
- **THEN** la respuesta es `403` con el código `ACCESO_DENEGADO`
- **AND** el turno queda sin cambios

### Requirement: Sobreturnos simultáneos por el último sillón libre

Si dos sobreturnos compiten por el último sillón libre, el sistema MUST aceptar como máximo uno. El otro MUST responder `409 SIN_SILLON_DISPONIBLE` y nunca un error `500`, aunque los dos hayan pasado la verificación del dominio al mismo tiempo.

#### Scenario: Dos sobreturnos al mismo tiempo
- **GIVEN** los usuarios `recepcion` y `ana.ejemplo` con sesión iniciada
- **AND** solo el Sillón 1 libre a las 10:00, con un turno previo que ocupa el Sillón 2 con margen
- **WHEN** los dos piden al mismo tiempo un sobreturno de "Consulta" con inicio 10:00
- **THEN** una respuesta es `201`
- **AND** la otra es `409` con el código `SIN_SILLON_DISPONIBLE`
- **AND** solo hay un sobreturno activo a las 10:00
