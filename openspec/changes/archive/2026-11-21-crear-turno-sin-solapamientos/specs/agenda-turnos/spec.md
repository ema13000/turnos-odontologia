# Spec Delta

## Purpose

Decidir si un turno nuevo puede entrar en la agenda del consultorio: calcular su intervalo y rechazarlo si choca con otro turno activo del mismo profesional o del mismo sillón, respetando el margen de limpieza y preparación, o si empieza en el pasado. En los escenarios, con datos ficticios y salvo que digan otra cosa, la prestación "Consulta" dura 30 minutos, el margen es de 10 minutos, los horarios son del 2026-12-01 y el momento actual es ese día a las 08:00, en `America/Argentina/Buenos_Aires`.

## ADDED Requirements

### Requirement: Intervalo del turno
El sistema SHALL calcular el fin de un turno nuevo como su inicio más la duración de la prestación, sin aceptar un fin ingresado a mano. El turno ocupa el intervalo semiabierto `[inicio, fin)`. Un turno aceptado queda en estado `RESERVADO`.

#### Scenario: Turno sin conflictos
- **GIVEN** una agenda sin turnos
- **WHEN** se crea un turno de "Consulta" con la Dra. A en el Sillón 1 a las 10:00
- **THEN** el turno se crea en estado `RESERVADO`
- **AND** su fin es las 10:30

### Requirement: Solo los turnos activos ocupan la agenda
El sistema SHALL considerar en la verificación de conflictos solo los turnos activos, es decir, en estado `RESERVADO` o `CONFIRMADO`. Un turno `CANCELADO` MUST NOT impedir la creación de otro turno.

#### Scenario: Un turno cancelado no bloquea el horario
- **GIVEN** un turno `CANCELADO` de 10:00 a 10:30 con la Dra. A en el Sillón 1
- **WHEN** se crea un turno de "Consulta" con la Dra. A en el Sillón 1 a las 10:00
- **THEN** el turno se crea en estado `RESERVADO`

### Requirement: Sin superposición por profesional
El sistema MUST rechazar un turno nuevo cuyo intervalo se superpone con el de un turno activo del mismo profesional, aunque sea en otro sillón, con el código `SOLAPAMIENTO_PROFESIONAL`.

#### Scenario: El profesional ya atiende en otro sillón
- **GIVEN** un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
- **WHEN** se crea un turno de "Consulta" con la Dra. A en el Sillón 2 a las 10:15
- **THEN** el turno se rechaza con `SOLAPAMIENTO_PROFESIONAL`

### Requirement: Sin superposición por sillón
El sistema MUST rechazar un turno nuevo cuyo intervalo se superpone con el de un turno activo en el mismo sillón, aunque sea con otro profesional, con el código `SOLAPAMIENTO_SILLON`.

#### Scenario: El sillón ya está ocupado por otro profesional
- **GIVEN** un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
- **WHEN** se crea un turno de "Consulta" con el Dr. B en el Sillón 1 a las 10:15
- **THEN** el turno se rechaza con `SOLAPAMIENTO_SILLON`

### Requirement: Margen entre turnos que comparten recurso
Entre un turno nuevo y un turno activo que comparten profesional o sillón MUST quedar libre al menos el margen configurado, antes y después. Si los intervalos no se superponen pero el margen no se cumple, el sistema MUST rechazar el turno con `MARGEN_INSUFICIENTE`. Los turnos que no comparten profesional ni sillón no tienen restricción de margen.

#### Scenario: Turno contiguo sin margen
- **GIVEN** un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
- **WHEN** se crea un turno de "Consulta" con la Dra. A en el Sillón 2 a las 10:30
- **THEN** el turno se rechaza con `MARGEN_INSUFICIENTE`

#### Scenario: Turno que respeta el margen exacto
- **GIVEN** un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
- **WHEN** se crea un turno de "Consulta" con la Dra. A en el Sillón 1 a las 10:40
- **THEN** el turno se crea en estado `RESERVADO`

#### Scenario: Turno contiguo sin recursos compartidos
- **GIVEN** un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
- **WHEN** se crea un turno de "Consulta" con el Dr. B en el Sillón 2 a las 10:30
- **THEN** el turno se crea en estado `RESERVADO`

### Requirement: Margen configurable con un mínimo de 10 minutos
El margen entre turnos SHALL ser configurable. El sistema MUST rechazar un margen menor a 10 minutos con el código `MARGEN_INVALIDO`. Un margen mayor amplía el tiempo libre exigido entre turnos que comparten recurso.

#### Scenario: Margen menor al mínimo
- **GIVEN** una configuración de margen de 8 minutos
- **WHEN** se intenta usar ese margen para crear turnos
- **THEN** la configuración se rechaza con `MARGEN_INVALIDO`

#### Scenario: Margen mayor al mínimo
- **GIVEN** un margen configurado de 15 minutos
- **AND** un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
- **WHEN** se crea un turno de "Consulta" con la Dra. A en el Sillón 1 a las 10:40
- **THEN** el turno se rechaza con `MARGEN_INSUFICIENTE`

### Requirement: Un solo error con prioridad fija
Si un turno nuevo tiene más de un conflicto, el sistema SHALL informar uno solo, con esta prioridad: `SOLAPAMIENTO_PROFESIONAL`, después `SOLAPAMIENTO_SILLON` y por último `MARGEN_INSUFICIENTE`.

#### Scenario: Choque por profesional y por sillón a la vez
- **GIVEN** un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
- **WHEN** se crea un turno de "Consulta" con la Dra. A en el Sillón 1 a las 10:15
- **THEN** el turno se rechaza con `SOLAPAMIENTO_PROFESIONAL`

### Requirement: Sin turnos en el pasado
El sistema MUST rechazar un turno nuevo cuyo inicio es anterior al momento actual, con el código `INICIO_EN_EL_PASADO`. Un turno que empieza exactamente en el momento actual SHALL aceptarse si no tiene otros conflictos.

#### Scenario: Inicio anterior al momento actual
- **GIVEN** que el momento actual es el 2026-12-01 a las 09:00
- **WHEN** se crea un turno de "Consulta" con la Dra. A en el Sillón 1 el 2026-12-01 a las 08:30
- **THEN** el turno se rechaza con `INICIO_EN_EL_PASADO`

#### Scenario: Inicio igual al momento actual
- **GIVEN** que el momento actual es el 2026-12-01 a las 09:00
- **AND** una agenda sin turnos
- **WHEN** se crea un turno de "Consulta" con la Dra. A en el Sillón 1 el 2026-12-01 a las 09:00
- **THEN** el turno se crea en estado `RESERVADO`
