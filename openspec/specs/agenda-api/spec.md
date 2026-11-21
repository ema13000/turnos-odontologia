# agenda-api Specification

## Purpose
Definir el contrato HTTP con el que el personal del consultorio crea, consulta, cancela y reprograma turnos bajo `/api/v1/turnos`, con los códigos de respuesta y de error que el frontend puede usar, y la garantía de que dos reservas simultáneas nunca dejan dos turnos superpuestos. En los escenarios, con datos ficticios, existen los usuarios `admin` (`ADMIN`), `recepcion` (`RECEPCIONISTA`) y `ana.ejemplo` (`ODONTOLOGO`), "Paciente Uno" (DNI `99000001`) con cuenta de paciente, la Dra. A, el Dr. B, el Sillón 1, el Sillón 2 y la prestación "Consulta" de 30 minutos. El margen es de 10 minutos y los horarios son del 2026-12-01, en `America/Argentina/Buenos_Aires`, con el momento actual ese día a las 08:00.

## Requirements

### Requirement: Crear un turno por la API
`POST /api/v1/turnos` con `pacienteId`, `profesionalId`, `sillonId`, `prestacionId` e `inicio` SHALL crear el turno con las reglas de la capacidad `agenda-turnos` y responder `201` con el id, los cuatro ids recibidos, `inicio`, `fin`, `estado` y `numeroReserva`. Las fechas MUST viajar en ISO-8601 con zona horaria.

#### Scenario: Recepción crea un turno
- **GIVEN** el usuario `recepcion` con sesión iniciada
- **AND** una agenda sin turnos
- **WHEN** pide `POST /api/v1/turnos` para "Paciente Uno" con la Dra. A, el Sillón 1, "Consulta" e inicio `2026-12-01T10:00:00-03:00`
- **THEN** la respuesta es `201`
- **AND** el cuerpo tiene `estado` `RESERVADO`, `fin` `2026-12-01T10:30:00-03:00` y un `numeroReserva`

### Requirement: Rechazos de negocio como 409
Cuando la agenda rechaza una operación, el sistema MUST responder `409` con un `ProblemDetail` que incluya la propiedad `codigo` con el código estable del rechazo (por ejemplo `SOLAPAMIENTO_PROFESIONAL`, `SOLAPAMIENTO_SILLON`, `MARGEN_INSUFICIENTE`, `INICIO_EN_EL_PASADO`, `RECURSO_INACTIVO`, `DATOS_DE_RESERVA_INVALIDOS` o `TURNO_NO_ACTIVO`) y un detalle legible en español.

#### Scenario: Turno que choca con la agenda del profesional
- **GIVEN** el usuario `recepcion` con sesión iniciada
- **AND** un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
- **WHEN** pide `POST /api/v1/turnos` con la Dra. A, el Sillón 2 e inicio a las 10:15
- **THEN** la respuesta es `409` con el código `SOLAPAMIENTO_PROFESIONAL`

#### Scenario: Turno contiguo sin margen
- **GIVEN** el usuario `recepcion` con sesión iniciada
- **AND** un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
- **WHEN** pide `POST /api/v1/turnos` con el Dr. B, el Sillón 1 e inicio a las 10:30
- **THEN** la respuesta es `409` con el código `MARGEN_INSUFICIENTE`

### Requirement: Datos de entrada inválidos como 422
El sistema MUST responder `422` con el código `DATOS_INVALIDOS` y la lista de campos con error cuando falta un dato obligatorio, una fecha no tiene formato ISO-8601 con zona horaria, o un id no corresponde a un paciente, profesional, sillón o prestación existente.

#### Scenario: Falta el inicio
- **GIVEN** el usuario `recepcion` con sesión iniciada
- **WHEN** pide `POST /api/v1/turnos` sin `inicio`
- **THEN** la respuesta es `422` con el código `DATOS_INVALIDOS`
- **AND** la lista de errores incluye el campo `inicio`

#### Scenario: Profesional inexistente
- **GIVEN** el usuario `recepcion` con sesión iniciada
- **WHEN** pide `POST /api/v1/turnos` con un `profesionalId` que no existe
- **THEN** la respuesta es `422` con el código `DATOS_INVALIDOS`
- **AND** la lista de errores incluye el campo `profesionalId`

### Requirement: Turno inexistente
Las operaciones sobre `/api/v1/turnos/{id}` MUST responder `404` con el código `TURNO_INEXISTENTE` cuando no existe un turno con ese id.

#### Scenario: Cancelar un turno que no existe
- **GIVEN** el usuario `recepcion` con sesión iniciada
- **WHEN** pide `POST /api/v1/turnos/999999/cancelar` con un DNI y un número de reserva
- **THEN** la respuesta es `404` con el código `TURNO_INEXISTENTE`

### Requirement: Consultar la agenda
`GET /api/v1/turnos?desde&hasta` SHALL responder `200` con los turnos activos cuyo intervalo se cruza con `[desde, hasta)`, ordenados por inicio, con el nombre del paciente. Acepta los filtros opcionales `profesionalId` y `sillonId`. `desde` y `hasta` son obligatorios. La respuesta MUST NOT incluir el número de reserva ni datos de salud.

#### Scenario: La agenda muestra solo los turnos activos
- **GIVEN** el usuario `ana.ejemplo` con sesión iniciada
- **AND** un turno activo a las 10:00 y un turno `CANCELADO` a las 11:00, los dos con la Dra. A
- **WHEN** pide `GET /api/v1/turnos` desde las 00:00 hasta las 23:59 del 2026-12-01
- **THEN** la respuesta es `200` con un solo turno, el de las 10:00
- **AND** el turno no incluye el número de reserva

#### Scenario: Filtro por profesional
- **GIVEN** el usuario `recepcion` con sesión iniciada
- **AND** un turno activo a las 10:00 con la Dra. A y otro a las 10:00 con el Dr. B
- **WHEN** pide `GET /api/v1/turnos` del 2026-12-01 con `profesionalId` de la Dra. A
- **THEN** la respuesta es `200` con un solo turno, el de la Dra. A

#### Scenario: Falta el período
- **GIVEN** el usuario `recepcion` con sesión iniciada
- **WHEN** pide `GET /api/v1/turnos` sin `desde`
- **THEN** la respuesta es `422` con el código `DATOS_INVALIDOS`

### Requirement: Cancelar y reprogramar por la API
`POST /api/v1/turnos/{id}/cancelar` con `dni` y `numeroReserva` SHALL aplicar la cancelación de `agenda-turnos` y responder `200` con el turno cancelado. `POST /api/v1/turnos/{id}/reprogramar` con `dni`, `numeroReserva`, `inicio` y, opcionalmente, `profesionalId` y `sillonId`, SHALL aplicar la reprogramación y responder `200` con el turno nuevo y su número de reserva.

#### Scenario: Recepción cancela un turno
- **GIVEN** el usuario `recepcion` con sesión iniciada
- **AND** un turno activo de "Paciente Uno" a las 10:00 con la Dra. A en el Sillón 1
- **WHEN** pide `POST /api/v1/turnos/{id}/cancelar` con el DNI `99000001` y el número de reserva del turno
- **THEN** la respuesta es `200` con `estado` `CANCELADO`
- **AND** un `POST /api/v1/turnos` con la Dra. A, el Sillón 1 e inicio a las 10:00 responde `201`

#### Scenario: Recepción reprograma un turno
- **GIVEN** el usuario `recepcion` con sesión iniciada
- **AND** un turno activo de "Paciente Uno" a las 10:00 con la Dra. A en el Sillón 1
- **WHEN** pide `POST /api/v1/turnos/{id}/reprogramar` con el DNI `99000001`, el número de reserva del turno e inicio a las 11:00
- **THEN** la respuesta es `200` con un turno de 11:00 a 11:30 en estado `RESERVADO` y un número de reserva nuevo
- **AND** el turno original queda en estado `CANCELADO`

#### Scenario: Reprogramación rechazada deja el turno intacto
- **GIVEN** el usuario `recepcion` con sesión iniciada
- **AND** un turno activo de "Paciente Uno" a las 10:00 con la Dra. A en el Sillón 1
- **AND** otro turno activo a las 11:00 con el Dr. B en el Sillón 1
- **WHEN** pide `POST /api/v1/turnos/{id}/reprogramar` del primero a las 11:10 con la Dra. A en el Sillón 1
- **THEN** la respuesta es `409` con el código `SOLAPAMIENTO_SILLON`
- **AND** el turno original sigue en estado `RESERVADO` a las 10:00

### Requirement: Reservas simultáneas
Si dos pedidos compiten por el mismo horario, el sistema MUST aceptar como máximo uno. El otro MUST responder `409` con `SOLAPAMIENTO_PROFESIONAL` o `SOLAPAMIENTO_SILLON`, según el recurso que ya está ocupado, y nunca con un error `500`. Esta garantía MUST valer aunque los dos pedidos pasen la verificación de la agenda al mismo tiempo.

#### Scenario: Dos reservas al mismo tiempo para el mismo sillón
- **GIVEN** los usuarios `recepcion` y `ana.ejemplo` con sesión iniciada
- **AND** una agenda sin turnos
- **WHEN** los dos piden al mismo tiempo `POST /api/v1/turnos` para el Sillón 1 a las 10:00, uno con la Dra. A y otro con el Dr. B
- **THEN** una respuesta es `201`
- **AND** la otra es `409` con el código `SOLAPAMIENTO_SILLON`
- **AND** la agenda tiene un solo turno activo a las 10:00 en el Sillón 1

### Requirement: Roles de la API de turnos
Las operaciones de `/api/v1/turnos` SHALL estar permitidas a los roles `ADMIN`, `RECEPCIONISTA` y `ODONTOLOGO`. Una sesión `PACIENTE` MUST recibir `403` con el código `ACCESO_DENEGADO` en todas ellas.

#### Scenario: El odontólogo crea un turno
- **GIVEN** el usuario `ana.ejemplo` con sesión iniciada
- **AND** una agenda sin turnos
- **WHEN** pide `POST /api/v1/turnos` con la Dra. A, el Sillón 1 e inicio a las 10:00
- **THEN** la respuesta es `201`

#### Scenario: Un paciente intenta crear un turno por la API del personal
- **GIVEN** "Paciente Uno" con sesión iniciada
- **WHEN** pide `POST /api/v1/turnos` con la Dra. A, el Sillón 1 e inicio a las 10:00
- **THEN** la respuesta es `403` con el código `ACCESO_DENEGADO`
- **AND** no se crea ningún turno
