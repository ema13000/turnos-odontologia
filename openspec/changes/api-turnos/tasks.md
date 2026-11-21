# Tasks

## 1. Aviso y migración V4

- [x] 1.1 Avisar a Juan Cruz que V4 cambia el `CHECK` de estado, el valor por defecto y los dos `EXCLUDE` de la tabla `turno`, y que se modifica `TurnoEntity` en `shared/persistence` (regla 3); verificar que el aviso quedó enviado antes de abrir el PR
- [x] 1.2 Escribir `V4__turnos_api.sql` según design D14 (estado `RESERVADO` y su valor por defecto, `EXCLUDE` recreados con `estado IN ('RESERVADO','CONFIRMADO')` y los mismos nombres, `codigo_reserva` con relleno, `NOT NULL` y `UNIQUE`, tabla `turno_auditoria`); verificar que la aplicación arranca contra el PostgreSQL de Docker y que Flyway informa V4 aplicada
- [x] 1.3 Agregar a `RestriccionesTurnoIntegrationTest` un test propio, que no modifique los existentes, que pruebe que un turno `AUSENTE` no bloquea el horario y un `RESERVADO` sí; verificar que pasa junto con los tests de C-03 en `./gradlew integrationTest`

## 2. Dominio: número de reserva y nuevos rechazos

- [x] 2.1 Agregar a `CodigoRechazo` `RECURSO_INACTIVO`, `DATOS_DE_RESERVA_INVALIDOS` y `TURNO_NO_ACTIVO`, y crear `ReferenciaInexistenteException(campo)` y `TurnoInexistenteException` (design D1); verificar que compila y que `agenda/domain` sigue sin importar Spring
- [x] 2.2 Crear `NumeroReserva` (formato `XXXX-XXXX`, alfabeto de 31 símbolos de D3) y el puerto `GeneradorNumeroReserva`; agregar `numeroReserva` a `Turno` y a `Turno.reservar`; verificar con un test unitario que se rechaza un valor con `0`, `O`, `1`, `I` o `L` o con otro formato
- [x] 2.3 Reemplazar `PrestacionRepository` por el puerto `RecursosAgenda` (D2) y adaptar `CrearTurno` al orden pasado → referencias inexistentes → recursos inactivos → conflictos; adaptar los dobles en memoria de C-01 (recursos activos por defecto, generador secuencial); verificar que los 11 escenarios de `CrearTurnoTest` y los 2 de `MargenTest` siguen en verde sin cambiar sus aserciones
- [x] 2.4 Escribir en `CrearTurnoTest` los escenarios "Turno nuevo con número de reserva" y "Dos turnos tienen números de reserva distintos"; verificar que pasan
- [x] 2.5 Escribir los escenarios "Profesional inactivo", "Sillón inactivo" y "Prestación inactiva"; verificar que pasan

## 3. Casos de uso Cancelar y Reprogramar

- [x] 3.1 Agregar `buscarParaModificar(id)` a `TurnoRepository` y su versión en memoria, e implementar `CancelarTurno` (validación conjunta de DNI y número de reserva con `MessageDigest.isEqual`, exigencia de turno activo; design D4); verificar que compila
- [x] 3.2 Escribir en `CancelarTurnoTest` los escenarios "DNI que no corresponde al turno", "Número de reserva que no corresponde al turno", "Cancelar libera el horario" y "Cancelar un turno ya cancelado"; verificar que pasan
- [x] 3.3 Implementar `ReprogramarTurno` (verifica el nuevo ignorando el original, después guarda el original cancelado y el nuevo; conserva paciente y prestación; D4); verificar que compila
- [x] 3.4 Escribir en `ReprogramarTurnoTest` los escenarios "Reprogramar sobre el propio horario", "Reprogramar a un horario en conflicto", "Reprogramar al pasado" y "Reprogramar un turno que no está activo"; verificar que pasan. El rollback del original en memoria se prueba acá a nivel de resultado; la atomicidad real se prueba en 5.4

## 4. Infraestructura: persistencia, configuración y auditoría

- [x] 4.1 Modificar `TurnoEntity` (`codigoReserva`, estado inicial `RESERVADO`, cambio de estado) y crear `TurnoAuditoriaEntity` (D14); verificar que la aplicación arranca con `ddl-auto: validate` contra la base con V4
- [x] 4.2 Implementar `TurnoRepositoryJpa` (`buscarQueIntersecan` con el filtro de D7, `guardar` con `saveAndFlush`, `fin_con_margen` y traducción del `EXCLUDE` de D6, `buscarParaModificar` con `PESSIMISTIC_WRITE`), `RecursosAgendaJpa` y `GeneradorNumeroReservaAleatorio` con reintentos (D3); verificar con un test de integración del adaptador que guardar directamente un turno que pisa el sillón de otro lanza `TurnoRechazadoException(SOLAPAMIENTO_SILLON)` y uno que pisa al profesional lanza `SOLAPAMIENTO_PROFESIONAL`
- [x] 4.3 Implementar el puerto `AuditoriaTurnos` con JPA y el puerto `ConsultaAgenda` con su read model `TurnoEnAgenda` (D11, D13); verificar que compila
- [x] 4.4 Crear `AgendaConfig` (propiedades `app.agenda.margen-minutos` y `app.zona-horaria`, bean `Clock` (los tests lo reemplazan con uno `@Primary`), beans de los casos de uso; D8) y `ServicioAgenda` transaccional que registra la auditoría y reintenta ante un deadlock (D5, D6); verificar que la aplicación arranca y que con `app.agenda.margen-minutos=8` el arranque falla con `MARGEN_INVALIDO`

## 5. API REST

- [x] 5.1 Crear los DTOs de D9, `TurnosController` con `@PreAuthorize` de clase (D12) y `AgendaExceptionHandler` (D10); crear `IntegracionAgendaBase` que extiende `IntegracionServidorBase` sin modificarla, con los datos ficticios y el `Clock` fijo el 2026-12-01 a las 08:00 (D15); verificar que compila
- [x] 5.2 Escribir en `TurnosApiIntegrationTest` los escenarios "Recepción crea un turno", "Turno que choca con la agenda del profesional", "Turno contiguo sin margen", "Falta el inicio", "Profesional inexistente" y "Cancelar un turno que no existe"; verificar que pasan con `./gradlew integrationTest`
- [x] 5.3 Escribir los escenarios "La agenda muestra solo los turnos activos", "Filtro por profesional" y "Falta el período"; verificar que pasan
- [x] 5.4 Escribir los escenarios "Recepción cancela un turno", "Recepción reprograma un turno" y "Reprogramación rechazada deja el turno intacto" (verificando en la base que el original no cambió); verificar que pasan
- [x] 5.5 Escribir los escenarios "El odontólogo crea un turno" y "Un paciente intenta crear un turno por la API del personal"; verificar que pasan
- [x] 5.6 Escribir el escenario "Dos reservas al mismo tiempo para el mismo sillón" con dos hilos y un `CountDownLatch` (D15); verificar que pasa 10 veces seguidas

## 6. Auditoría

- [x] 6.1 Escribir en `AuditoriaTurnosIntegrationTest` los escenarios "Se registra la creación", "Se registra la cancelación" y "Se registra la reprogramación"; verificar que pasan
- [x] 6.2 Escribir los escenarios "Reprogramación rechazada sin registro" y "La cancelación no guarda el DNI ni el número de reserva"; verificar que pasan

## 7. Verificación integral

- [x] 7.1 Correr `./gradlew test` y `./gradlew integrationTest` completos y verificar que todo pasa, que hay un test por cada uno de los 33 escenarios del change con `@DisplayName` igual a su nombre, y que los tests de C-01, C-03 y C-05 siguen en verde
- [x] 7.2 Verificar con `grep` que `agenda/domain` y `agenda/application` no importan `org.springframework` ni `jakarta.persistence`, y que ningún log ni registro de auditoría incluye DNI, número de reserva o datos de salud
- [x] 7.3 Correr `openspec validate api-turnos --strict` y verificar que es válido

## Workflow follow-up

- Abrir el PR de `change/C-06-api-turnos` a `master`, revisarlo y mergearlo.
- Marcar `[x]` en C-06 de `CHANGES.md`.
- Archivar el change con `openspec archive api-turnos` y verificar los specs archivados (`agenda-turnos` actualizado, `agenda-api` y `auditoria-turnos` nuevos).
- Avisar a Enzo (C-09) que los endpoints están en `master`.
