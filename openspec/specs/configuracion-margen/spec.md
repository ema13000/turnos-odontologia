# configuracion-margen Specification

## Purpose

Permitir que el administrador consulte y cambie el margen de limpieza y preparación entre turnos que comparten profesional o sillón, sin bajarlo nunca de 10 minutos (RN-AG-04), y que la agenda use siempre el valor vigente.

## Requirements

### Requirement: Margen predeterminado
El sistema SHALL tener un margen entre turnos de 10 minutos mientras el administrador no lo cambie.

#### Scenario: Consultar el margen sin haberlo cambiado
- **GIVEN** un consultorio recién instalado
- **WHEN** el administrador consulta el margen
- **THEN** el margen es de 10 minutos

### Requirement: Cambiar el margen con un mínimo de 10 minutos
El sistema SHALL permitir que el administrador cambie el margen a un número entero de minutos mayor o igual a 10. Un valor menor MUST rechazarse con `422` y el código `MARGEN_INVALIDO`, sin cambiar el margen vigente.

#### Scenario: Aumentar el margen
- **GIVEN** un margen de 10 minutos
- **WHEN** el administrador cambia el margen a 15 minutos
- **THEN** el margen vigente es de 15 minutos

#### Scenario: Margen menor al mínimo
- **GIVEN** un margen de 15 minutos
- **WHEN** el administrador cambia el margen a 8 minutos
- **THEN** el cambio se rechaza con `422` y el código `MARGEN_INVALIDO`
- **AND** el margen vigente sigue siendo de 15 minutos

#### Scenario: Margen igual al mínimo
- **GIVEN** un margen de 15 minutos
- **WHEN** el administrador cambia el margen a 10 minutos
- **THEN** el margen vigente es de 10 minutos

### Requirement: El margen vigente se conserva
El sistema MUST conservar el margen vigente entre reinicios de la aplicación, y la agenda SHALL usar ese valor para verificar los turnos que se creen después del cambio.

#### Scenario: Margen después de reiniciar
- **GIVEN** que el administrador cambió el margen a 15 minutos
- **WHEN** la aplicación se reinicia
- **THEN** el margen vigente sigue siendo de 15 minutos

### Requirement: Solo el administrador cambia el margen
El sistema SHALL permitir consultar el margen a los roles `ADMIN`, `RECEPCIONISTA` y `ODONTOLOGO`, y MUST permitir cambiarlo solo al rol `ADMIN`. Otro rol recibe `403` con el código `ACCESO_DENEGADO` y el margen no cambia.

#### Scenario: Recepción consulta el margen
- **GIVEN** un margen de 10 minutos
- **AND** el usuario `recepcion` con sesión iniciada
- **WHEN** consulta el margen
- **THEN** la respuesta es `200` con 10 minutos

#### Scenario: Odontólogo intenta cambiar el margen
- **GIVEN** un margen de 10 minutos
- **AND** el usuario `ana.ejemplo` (`ODONTOLOGO`) con sesión iniciada
- **WHEN** cambia el margen a 20 minutos
- **THEN** la respuesta es `403` con el código `ACCESO_DENEGADO`
- **AND** el margen vigente sigue siendo de 10 minutos
