# agenda-turnos Specification

## Purpose
Decidir si un turno nuevo puede entrar en la agenda del consultorio: calcular su intervalo y rechazarlo si choca con otro turno activo del mismo profesional o del mismo sillón, respetando el margen de limpieza y preparación, o si empieza en el pasado. En los escenarios, con datos ficticios y salvo que digan otra cosa, la prestación "Consulta" dura 30 minutos, el margen es de 10 minutos, los horarios son del 2026-12-01 y el momento actual es ese día a las 08:00, en `America/Argentina/Buenos_Aires`.

## Requirements

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

### Requirement: Número de reserva
El sistema SHALL asignar a cada turno nuevo un número de reserva único, de 8 caracteres aleatorios con el formato `XXXX-XXXX`, tomados de letras mayúsculas y dígitos sin los caracteres ambiguos `0`, `O`, `1`, `I` y `L`. El número de reserva MUST NOT derivarse del id del turno ni de otro dato predecible.

#### Scenario: Turno nuevo con número de reserva
- **GIVEN** una agenda sin turnos
- **WHEN** se crea un turno de "Consulta" con la Dra. A en el Sillón 1 a las 10:00
- **THEN** el turno tiene un número de reserva con el formato `XXXX-XXXX` y sin caracteres ambiguos

#### Scenario: Dos turnos tienen números de reserva distintos
- **GIVEN** una agenda sin turnos
- **WHEN** se crean un turno con la Dra. A en el Sillón 1 a las 10:00 y otro con el Dr. B en el Sillón 2 a las 10:00
- **THEN** los dos turnos tienen números de reserva distintos

### Requirement: Sin turnos con recursos inactivos
El sistema MUST rechazar un turno nuevo con el código `RECURSO_INACTIVO` si su profesional, su sillón o su prestación están inactivos.

#### Scenario: Profesional inactivo
- **GIVEN** que la Dra. A está inactiva
- **WHEN** se crea un turno de "Consulta" con la Dra. A en el Sillón 1 a las 10:00
- **THEN** el turno se rechaza con `RECURSO_INACTIVO`

#### Scenario: Sillón inactivo
- **GIVEN** que el Sillón 1 está inactivo
- **WHEN** se crea un turno de "Consulta" con la Dra. A en el Sillón 1 a las 10:00
- **THEN** el turno se rechaza con `RECURSO_INACTIVO`

#### Scenario: Prestación inactiva
- **GIVEN** que la prestación "Consulta" está inactiva
- **WHEN** se crea un turno de "Consulta" con la Dra. A en el Sillón 1 a las 10:00
- **THEN** el turno se rechaza con `RECURSO_INACTIVO`

### Requirement: Validación de DNI y número de reserva
Para cancelar o reprogramar un turno, el personal MUST informar el DNI del paciente y el número de reserva del turno. Si alguno de los dos no coincide con el turno, el sistema MUST rechazar la operación con el código `DATOS_DE_RESERVA_INVALIDOS`, sin indicar cuál de los dos falló, y el turno no cambia.

#### Scenario: DNI que no corresponde al turno
- **GIVEN** un turno activo de "Paciente Uno" (DNI `99000001`) de 10:00 a 10:30
- **WHEN** el personal lo cancela informando el DNI `99000002` y el número de reserva correcto
- **THEN** la operación se rechaza con `DATOS_DE_RESERVA_INVALIDOS`
- **AND** el turno sigue en estado `RESERVADO`

#### Scenario: Número de reserva que no corresponde al turno
- **GIVEN** un turno activo de "Paciente Uno" (DNI `99000001`) de 10:00 a 10:30
- **WHEN** el personal lo cancela informando el DNI `99000001` y un número de reserva de otro turno
- **THEN** la operación se rechaza con `DATOS_DE_RESERVA_INVALIDOS`
- **AND** el turno sigue en estado `RESERVADO`

### Requirement: Cancelar un turno
El personal SHALL poder cancelar un turno activo en cualquier momento. El turno pasa a `CANCELADO` y su horario queda libre de inmediato para otro turno. Cancelar un turno que no está activo MUST rechazarse con el código `TURNO_NO_ACTIVO`.

#### Scenario: Cancelar libera el horario
- **GIVEN** un turno activo de "Paciente Uno" de 10:00 a 10:30 con la Dra. A en el Sillón 1
- **WHEN** el personal lo cancela con el DNI y el número de reserva correctos
- **THEN** el turno queda en estado `CANCELADO`
- **AND** se puede crear otro turno de "Consulta" con la Dra. A en el Sillón 1 a las 10:00

#### Scenario: Cancelar un turno ya cancelado
- **GIVEN** un turno `CANCELADO` de "Paciente Uno" de 10:00 a 10:30
- **WHEN** el personal lo cancela con el DNI y el número de reserva correctos
- **THEN** la operación se rechaza con `TURNO_NO_ACTIVO`

### Requirement: Reprogramar un turno
El personal SHALL poder reprogramar un turno activo a un nuevo inicio y, opcionalmente, a otro profesional o sillón. El turno original pasa a `CANCELADO` y se crea un turno nuevo, con las mismas reglas que la creación y un número de reserva nuevo. Los dos cambios ocurren juntos o no ocurre ninguno. El turno original no cuenta como conflicto para el nuevo.

#### Scenario: Reprogramar sobre el propio horario
- **GIVEN** un turno activo de "Paciente Uno" de 10:00 a 10:30 con la Dra. A en el Sillón 1
- **WHEN** el personal lo reprograma a las 10:15 con la Dra. A en el Sillón 1
- **THEN** el turno original queda en estado `CANCELADO`
- **AND** existe un turno nuevo de "Paciente Uno" de 10:15 a 10:45 en estado `RESERVADO`
- **AND** el turno nuevo tiene un número de reserva distinto al original

#### Scenario: Reprogramar a un horario en conflicto
- **GIVEN** un turno activo de "Paciente Uno" de 10:00 a 10:30 con la Dra. A en el Sillón 1
- **AND** otro turno activo de 11:00 a 11:30 con la Dra. A en el Sillón 2
- **WHEN** el personal reprograma el primero a las 11:15 con la Dra. A en el Sillón 1
- **THEN** la operación se rechaza con `SOLAPAMIENTO_PROFESIONAL`
- **AND** el turno original sigue en estado `RESERVADO` de 10:00 a 10:30
- **AND** no se crea ningún turno nuevo

#### Scenario: Reprogramar al pasado
- **GIVEN** que el momento actual es el 2026-12-01 a las 09:00
- **AND** un turno activo de "Paciente Uno" de 10:00 a 10:30 con la Dra. A en el Sillón 1
- **WHEN** el personal lo reprograma a las 08:30
- **THEN** la operación se rechaza con `INICIO_EN_EL_PASADO`
- **AND** el turno original sigue en estado `RESERVADO`

#### Scenario: Reprogramar un turno que no está activo
- **GIVEN** un turno `CANCELADO` de "Paciente Uno" de 10:00 a 10:30
- **WHEN** el personal lo reprograma a las 11:00 con el DNI y el número de reserva correctos
- **THEN** la operación se rechaza con `TURNO_NO_ACTIVO`
