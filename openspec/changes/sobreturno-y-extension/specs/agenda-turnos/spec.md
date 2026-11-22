# Spec Delta

## ADDED Requirements

### Requirement: Sobreturno con motivo obligatorio

El sistema SHALL permitir cargar un sobreturno solo desde el personal y MUST exigir un `motivo` administrativo no vacío. El motivo es administrativo, nunca clínico. El turno resultante MUST quedar marcado como sobreturno. Un motivo vacío MUST rechazarse con `DATOS_INVALIDOS`. La excepción no se aplica a los turnos normales.

#### Scenario: Sobreturno con motivo válido
- **GIVEN** una agenda sin otros turnos y un motivo administrativo "Urgencia con el Paciente Uno"
- **WHEN** el personal crea un sobreturno de "Consulta" con la Dra. A a las 10:00
- **THEN** el turno se crea en estado `RESERVADO` con `esSobreturno` verdadero
- **AND** el motivo queda asociado al turno, sin exponerse en la respuesta

#### Scenario: Sobreturno sin motivo
- **GIVEN** el personal con sesión iniciada
- **WHEN** crea un sobreturno con el motivo vacío o solo espacios
- **THEN** la operación se rechaza con `DATOS_INVALIDOS`
- **AND** no se crea ningún turno

### Requirement: Asignación automática de sillón para sobreturnos

Para un sobreturno, el sistema SHALL asignar el sillón en el servidor: prefiere el sillón habitual del profesional si está libre con margen, y si no, cualquier sillón activo libre en orden determinista. Mientras C-04/V2 no exponga el sillón habitual, se aplica directamente el orden determinista. El cliente MUST NOT elegir el sillón. Si ningún sillón está libre con el margen de limpieza, el sistema MUST rechazar con `SIN_SILLON_DISPONIBLE`.

#### Scenario: Elige el sillón libre en orden determinista
- **GIVEN** el Sillón 1 y el Sillón 2 activos y libres, sin información de sillón habitual disponible
- **WHEN** el personal crea un sobreturno de "Consulta" con la Dra. A a las 10:00
- **THEN** el sillón asignado es el Sillón 1
- **AND** una operación idéntica repetida más tarde vuelve a elegir el Sillón 1 si sigue libre

#### Scenario: Usa otro sillón cuando el primero está ocupado
- **GIVEN** un turno activo de 10:00 a 10:30 con el Dr. B en el Sillón 1
- **AND** el Sillón 2 activo y libre
- **WHEN** el personal crea un sobreturno de "Consulta" con la Dra. A a las 10:10
- **THEN** el sobreturno se crea en el Sillón 2 con fin 10:40

#### Scenario: Sin sillón libre con margen de limpieza
- **GIVEN** turnos activos de 10:00 a 10:30 en el Sillón 1 y en el Sillón 2
- **AND** un margen de 10 minutos
- **WHEN** el personal crea un sobreturno de "Consulta" a las 10:35
- **THEN** la operación se rechaza con `SIN_SILLON_DISPONIBLE`
- **AND** no se crea ningún turno

#### Scenario: El margen de limpieza del sillón es obligatorio
- **GIVEN** turnos activos que terminan a las 10:30 en el Sillón 1 y en el Sillón 2
- **WHEN** el personal crea un sobreturno de "Consulta" a las 10:30 con margen de 10 minutos
- **THEN** la operación se rechaza con `SIN_SILLON_DISPONIBLE`

#### Scenario: Un sillón tomado en una carrera se descarta y se prueba el siguiente
- **GIVEN** el Sillón 1 y el Sillón 2 activos y libres a las 10:00
- **AND** el Sillón 1 es tomado por otra operación después de la verificación del dominio
- **WHEN** el personal crea un sobreturno de "Consulta" con la Dra. A a las 10:00
- **THEN** el sobreturno se crea en el Sillón 2
- **AND** solo si no queda ningún sillón libre se rechaza con `SIN_SILLON_DISPONIBLE`

### Requirement: Excepción de sobreturno solo para la agenda del profesional

Un sobreturno SHALL poder superponerse con turnos activos del mismo profesional, pero MUST NOT superponerse con el intervalo de limpieza de ningún sillón. La excepción se aplica únicamente a la restricción del profesional; la del sillón, incluido su margen, sigue vigente. Los turnos normales conservan ambas restricciones.

#### Scenario: Sobreturno que pisa la agenda del profesional en otro sillón
- **GIVEN** un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
- **AND** el Sillón 2 activo y libre
- **WHEN** el personal crea un sobreturno de "Consulta" con la Dra. A a las 10:15
- **THEN** el sobreturno se crea en el Sillón 2 con fin 10:45
- **AND** los dos turnos de la Dra. A siguen activos

#### Scenario: Sobreturno que pisa el sillón se rechaza
- **GIVEN** un turno activo de 10:00 a 10:30 con el Dr. B en el Sillón 1
- **WHEN** el personal pide un sobreturno de "Consulta" a las 10:10 que solo tiene libre el Sillón 1
- **THEN** la operación se rechaza con `SIN_SILLON_DISPONIBLE`

### Requirement: Extensión con fin absoluto y margen recalculado

El sistema SHALL permitir al personal extender el fin de un turno `RESERVADO` o `CONFIRMADO` informando un `fin` absoluto y un `motivo` administrativo. El nuevo fin MUST ser posterior al fin actual. La extensión MUST recalcular `fin_con_margen = fin + margen vigente`, validar profesional, sillón y margen como un turno nuevo, marcar `extendido` y conservar el `numeroReserva`. MUST NOT extenderse un sobreturno ni un turno inactivo.

#### Scenario: Extensión sin conflictos
- **GIVEN** un turno activo de "Consulta" de 10:00 a 10:30 con la Dra. A en el Sillón 1
- **AND** un margen de 10 minutos
- **WHEN** el personal extiende el turno con un `fin` absoluto de 10:45 y un motivo administrativo
- **THEN** el turno pasa a terminar a las 10:45 con `fin_con_margen` 10:55 y `extendido` verdadero
- **AND** conserva su `numeroReserva` original y sigue en estado `RESERVADO`

#### Scenario: Extensión que choca con el sillón
- **GIVEN** un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
- **AND** otro turno activo de 11:00 a 11:30 con el Dr. B en el Sillón 1
- **WHEN** el personal extiende el primero con un `fin` absoluto de 11:00
- **THEN** la operación se rechaza con `SOLAPAMIENTO_SILLON`
- **AND** el turno original sigue de 10:00 a 10:30 con su `numeroReserva`

#### Scenario: Extensión sin margen suficiente
- **GIVEN** un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
- **AND** otro turno activo de 10:45 a 11:15 con el Dr. B en el Sillón 1
- **WHEN** el personal extiende el primero con un `fin` absoluto de 10:40
- **THEN** la operación se rechaza con `MARGEN_INSUFICIENTE`

#### Scenario: Extensión de un sobreturno
- **GIVEN** un sobreturno activo de 10:00 a 10:30
- **WHEN** el personal pide extenderlo con un `fin` absoluto de 10:45
- **THEN** la operación se rechaza con `TURNO_NO_EXTENSIBLE`
- **AND** el sobreturno queda sin cambios

#### Scenario: Extensión de un turno inactivo
- **GIVEN** un turno `CANCELADO` de 10:00 a 10:30
- **WHEN** el personal pide extenderlo con un `fin` absoluto de 10:45
- **THEN** la operación se rechaza con `TURNO_NO_ACTIVO`

#### Scenario: Extensión con un fin no posterior
- **GIVEN** un turno activo de 10:00 a 10:30
- **WHEN** el personal pide extenderlo con un `fin` absoluto de 10:30 o de 10:20
- **THEN** la operación se rechaza con `DATOS_INVALIDOS`
- **AND** el turno queda sin cambios

#### Scenario: Extensión de un turno ya iniciado
- **GIVEN** un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1 y el reloj en las 10:20
- **WHEN** el personal pide extenderlo con un `fin` absoluto de 10:45 y un motivo administrativo
- **THEN** el turno pasa a terminar a las 10:45 con `extendido` verdadero
- **AND** conserva su `numeroReserva` original y su estado

#### Scenario: Extensión de un turno ya iniciado con un fin que no está en el futuro
- **GIVEN** un turno activo de 10:00 a 10:30 y el reloj en las 10:50
- **WHEN** el personal pide extenderlo con un `fin` absoluto de 10:45
- **THEN** la operación se rechaza con `DATOS_INVALIDOS`
- **AND** el turno queda sin cambios

#### Scenario: Extender un turno iniciado no habilita crear turnos en el pasado
- **GIVEN** el reloj en las 10:20 y un turno activo ya iniciado de 10:00 a 10:30
- **WHEN** el personal pide crear un turno nuevo con inicio 10:00
- **THEN** la operación se rechaza con `INICIO_EN_EL_PASADO`
- **AND** no se crea ningún turno

### Requirement: Atomicidad y doble garantía en sobreturnos y extensiones

Cada sobreturno y cada extensión SHALL ser atómicos junto con su registro de auditoría: si la operación se rechaza, no queda turno nuevo, ni extensión, ni auditoría. La verificación del dominio y las dos restricciones `EXCLUDE USING gist` de PostgreSQL MUST aplicarse simultáneamente: la de sillón incluye a los sobreturnos y la de profesional los excluye.

#### Scenario: Rechazo sin efectos parciales
- **GIVEN** turnos activos que ocupan los dos sillones de 10:00 a 10:30
- **WHEN** el personal intenta crear un sobreturno a las 10:10 y la operación se rechaza
- **THEN** no existe un turno sobreturno nuevo
- **AND** no existe ningún registro de auditoría de esa operación

#### Scenario: La restricción del sillón incluye a los sobreturnos
- **GIVEN** un sobreturno activo de 10:00 a 10:30 en el Sillón 1
- **WHEN** se intenta insertar otro turno activo en el Sillón 1 a las 10:25
- **THEN** la base rechaza la inserción por `turno_sin_solapamiento_sillon`
