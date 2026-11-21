# Design

## Context

- **C-01** dejó el dominio y el caso de uso `CrearTurno`, sin Spring, en `agenda/domain` y `agenda/application`, con dos puertos: `TurnoRepository` (`buscarQueIntersecan`, `guardar`) y `PrestacionRepository` (`duracionDe`).
- **C-03** dejó las entidades JPA en `shared/persistence` (módulo de Juan Cruz) y V1 con la tabla `turno`. Esa tabla guarda `fin_con_margen` en cada fila, y las restricciones `EXCLUDE` usan `tstzrange(inicio, fin_con_margen)`. Con el mismo margen, eso equivale a la regla del dominio `N.inicio < E.fin + M && E.inicio < N.fin + M`.
- **C-05** dejó la seguridad:
  - sesión con cookie y CSRF;
  - `/api/v1/**` cerrado al rol `PACIENTE`;
  - `@EnableMethodSecurity` activo;
  - `UsuarioAutenticado` como principal, con el `username`.

  Su `UsuariosExceptionHandler` se limita al paquete `usuarios.api` y espera el manejador de C-06.
- Los tests de integración de otros módulos limpian con `TRUNCATE ... turno ... CASCADE`. Eso también vacía `turno_auditoria`, que tiene FK a `turno`.
- Todavía no existe un `Clock` como bean ni configuración de zona horaria para la API.
- Motivación y alcance: ver `proposal.md`. Requisitos: ver `specs/`.

## Goals / Non-Goals

**Goals:**
- Que la doble garantía contra solapamientos funcione de punta a punta: dominio, adaptador, `EXCLUDE` y respuesta `409`.
- Que crear, cancelar y reprogramar sean atómicos, auditoría incluida.
- Mantener el dominio y la aplicación sin Spring, como en C-01, y los tests de C-01 en verde sin debilitarlos.

**Non-Goals:**
- Un manejador de errores global para todo el proyecto: el de C-06 se limita a `agenda.api`, como hizo C-05 con su paquete.
- Paginación de la agenda: un consultorio chico tiene pocas decenas de turnos por día.
- Generalizar el bloqueo de filas para otros módulos.

## Decisions

### D1. Los nuevos rechazos se suman a `CodigoRechazo`
Se agregan `RECURSO_INACTIVO`, `DATOS_DE_RESERVA_INVALIDOS` y `TURNO_NO_ACTIVO` al enum existente, y se siguen lanzando con `TurnoRechazadoException`. Las referencias inexistentes (`422`) y el turno inexistente (`404`) **no** son rechazos de la agenda: tienen excepciones propias, `ReferenciaInexistenteException(campo)` y `TurnoInexistenteException`.
*Alternativa descartada*: una excepción por código. Multiplica las clases sin aportar nada, porque el manejador traduce por código.

### D2. Los recursos inactivos se verifican en `CrearTurno` con un puerto nuevo
Se agrega el puerto `RecursosAgenda`. Para un comando devuelve la duración de la prestación y si el paciente, el profesional, el sillón y la prestación existen y están activos. Ese puerto reemplaza a `PrestacionRepository`, que queda absorbido.
- **Orden de verificación**: inicio en el pasado → referencias inexistentes → recursos inactivos → conflictos de agenda. El pasado va primero, como en C-01, porque no necesita consultar nada.
- **Doble de prueba**: el doble en memoria de C-01 se adapta. Todos los recursos están activos por defecto, así los 11 escenarios de C-01 no cambian de significado.

*Alternativa descartada*: verificar en el controller. Dejaría la regla RN-AG-11 fuera del dominio y la reserva online de C-10 tendría que repetirla.

### D3. El número de reserva es un objeto de valor con su generador detrás de un puerto
- **`NumeroReserva`** (record) valida el formato `XXXX-XXXX` con el alfabeto `ABCDEFGHJKMNPQRSTUVWXYZ23456789`: 31 símbolos, sin `0 O 1 I L`. Eso da 31⁸ ≈ 8,5 × 10¹¹ combinaciones.
- **`Turno`** suma el campo `numeroReserva`, y `Turno.reservar(...)` lo recibe.
- **Generador**: el puerto `GeneradorNumeroReserva` se implementa con `SecureRandom` en infraestructura. Los tests usan un generador secuencial, para tener resultados deterministas.
- **Colisiones**: antes de guardar, el adaptador consulta si el número ya existe y, si existe, genera otro (hasta 3 intentos). La restricción `UNIQUE` de la base es la garantía final.

*Alternativa descartada*: derivarlo del id con un hash. Sería predecible, y eso lo prohíbe el spec.

### D4. Cancelar y reprogramar son casos de uso nuevos, sin Spring
- **`CancelarTurno`**: busca el turno con bloqueo, valida el DNI y el número de reserva, exige que el turno esté activo y lo pasa a `CANCELADO`.
- **`ReprogramarTurno`**: hace las mismas validaciones sobre el original. Después pide a `CrearTurno` que **arme y verifique el turno nuevo sin guardarlo, ignorando el original** como conflicto (`preparar(comando, idOriginal)`). Si pasa, guarda el original cancelado y recién entonces inserta el nuevo, así el `EXCLUDE` no los ve superpuestos. Con esto se cumple "Reprogramar sobre el propio horario", y un rechazo del dominio deja el original intacto incluso sin transacción. El turno nuevo conserva el paciente y la prestación. El profesional y el sillón se cambian solo si el pedido los trae.

  *Ajuste durante el apply*: el orden original era "cancelar y guardar, después crear". Se cambió porque, sin rollback (por ejemplo, con los dobles en memoria), un rechazo dejaba el original cancelado, y porque en la base no se puede reparar a mano una transacción abortada por el `EXCLUDE`.
- **Validación**: el DNI y el número de reserva se comparan juntos, y cualquier diferencia da el mismo `DATOS_DE_RESERVA_INVALIDOS`. El número se compara con `MessageDigest.isEqual`, para no filtrar información por tiempo de respuesta.
- **Puerto**: `TurnoRepository` suma `buscarParaModificar(id)`, que devuelve el turno y el DNI de su paciente y bloquea la fila.

### D5. La transacción la abre un servicio de infraestructura
`ServicioAgenda` (`@Service`) en `agenda/infrastructure` arma los casos de uso y, en la misma transacción, registra la auditoría. La transacción la abre con un `TransactionTemplate`, no con `@Transactional`, porque el reintento de D6 tiene que quedar fuera de ella. El controller solo llama a este servicio.
- Si cualquier paso lanza una excepción, se hace rollback de todo: el turno original de una reprogramación y el registro de auditoría vuelven atrás. Eso cumple "Reprogramación rechazada deja el turno intacto" y "Solo se auditan las operaciones que ocurrieron".
- Los beans de los casos de uso (`CrearTurno`, `CancelarTurno`, `ReprogramarTurno`) se declaran en `AgendaConfig`, con el `Clock` y el `Margen`.

*Alternativa descartada*: `@Transactional` en los casos de uso. Metería Spring en `agenda/application`, contra la decisión de C-01.

### D6. La violación del `EXCLUDE` se traduce en el adaptador
`TurnoRepositoryJpa.guardar` usa `saveAndFlush`, que hace que la restricción salte dentro del caso de uso. El adaptador captura `DataIntegrityViolationException` y la traduce según el nombre de la restricción, con el mismo método que `UsuariosExceptionHandler`:
- `turno_sin_solapamiento_profesional` → `TurnoRechazadoException(SOLAPAMIENTO_PROFESIONAL)`;
- `turno_sin_solapamiento_sillon` → `TurnoRechazadoException(SOLAPAMIENTO_SILLON)`;
- cualquier otra restricción se vuelve a lanzar.

*Alternativa descartada*: traducirla en el `@RestControllerAdvice`. Funcionaría para la API, pero el error dejaría de ser del dominio, y la reserva online de C-10 tendría que repetir la traducción.

**Cómo se resuelve una carrera**: con `READ COMMITTED`, dos inserciones que chocan se bloquean. La segunda espera a que la primera confirme y recién ahí falla. Por eso, en el escenario "Dos reservas al mismo tiempo", el resultado siempre es un `201` y un `409 SOLAPAMIENTO_SILLON`, gane quien gane. Si el segundo pedido llega cuando el primero ya confirmó, lo rechaza el dominio con el mismo código.

*Ajuste durante el apply*: el test de concurrencia mostró que, cuando las dos inserciones llegan a la vez, cada una verifica el `EXCLUDE` contra la fila todavía no confirmada de la otra, y las dos quedan esperándose. PostgreSQL detecta el **deadlock** (`40P01`) y aborta una, que llega como `CannotAcquireLockException` y respondía `500`. `ServicioAgenda` ahora reintenta la transacción entera ante una `PessimisticLockingFailureException`, hasta 3 intentos. En el reintento el otro turno ya está confirmado y el dominio responde el `409` correcto. Se verificó con 30 repeticiones del escenario.

### D7. El adaptador calcula `fin_con_margen` con el margen vigente
`fin_con_margen = fin + margen`, con el `Margen` de la configuración. La consulta `buscarQueIntersecan` filtra `estado IN ('RESERVADO','CONFIRMADO')`, `profesional_id = ? OR sillon_id = ?`, `inicio < ventana.fin AND fin > ventana.inicio`, con la ventana ya ampliada por C-01.

### D8. El margen y la zona horaria salen de propiedades hasta C-04
- `app.agenda.margen-minutos`, con valor por defecto 10, se convierte con `Margen.deMinutos` al arrancar. Un valor menor a 10 hace fallar el arranque con `MARGEN_INVALIDO`. Ese es el escenario "Margen menor al mínimo" del spec de C-01, ahora como configuración.
- `app.zona-horaria`, con valor por defecto `America/Argentina/Buenos_Aires`, alimenta el bean `Clock` (los tests lo reemplazan por uno fijo marcado `@Primary`; `@ConditionalOnMissingBean` no es confiable fuera de una autoconfiguración) y la conversión de las respuestas.
- C-04 va a reemplazar la propiedad del margen por la tabla `configuracion`, sin tocar el dominio.

### D9. La API usa `OffsetDateTime` en el borde y `Instant` adentro
- Los pedidos reciben `OffsetDateTime`, así una fecha sin zona falla al leerse y responde `422`.
- El dominio sigue con `Instant`.
- Las respuestas convierten a `OffsetDateTime` en la zona configurada. Por eso el spec puede exigir `2026-12-01T10:30:00-03:00`.

**DTOs** (records en `agenda/api`):

| DTO | Contenido |
|---|---|
| `CrearTurnoRequest` | Con Bean Validation. |
| `TurnoResponse` | Incluye el número de reserva. Se usa al crear, cancelar y reprogramar. |
| `TurnoAgendaResponse` | Para la consulta de la agenda. Incluye el nombre y el apellido del paciente y **no** el número de reserva ni el DNI. |
| `CancelarTurnoRequest` | DNI y número de reserva. |
| `ReprogramarTurnoRequest` | DNI, número de reserva, nuevo inicio y, opcionalmente, profesional y sillón. |

### D10. Un manejador de errores para `agenda.api`
`AgendaExceptionHandler` (`@RestControllerAdvice(basePackages = "ar.turnos.odontologia.agenda.api")`) responde `ProblemDetail` con la propiedad `codigo`, con el mismo formato de C-05:

| Excepción | Respuesta |
|---|---|
| `TurnoRechazadoException` | `409` + su código, con detalle en español |
| `TurnoInexistenteException` | `404 TURNO_INEXISTENTE` |
| `ReferenciaInexistenteException` | `422 DATOS_INVALIDOS` con el campo en `errores` |
| `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException` | `422 DATOS_INVALIDOS` |

### D11. La consulta de la agenda es una lectura aparte
La consulta tiene su propio puerto, `ConsultaAgenda.turnosActivos(desde, hasta, profesionalId?, sillonId?)`, con un read model `TurnoEnAgenda` y una consulta JPQL con join a paciente.
- No pasa por el dominio, porque no decide nada.
- Ordena por `inicio`.
- Usa el mismo criterio de "activo" y de cruce de intervalos que D7.

### D12. Los roles se declaran en el controller
`TurnosController` lleva `@PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA','ODONTOLOGO')")` a nivel de clase, como pide `seguridad-autorizacion` ("Permisos por rol en cada operación del personal"). `SecurityConfig` no cambia.

### D13. La auditoría es un puerto que llama el servicio
- **Puerto**: `AuditoriaTurnos.registrar(turnoId, accion, actor, turnoRelacionadoId?)`, implementado con JPA sobre `turno_auditoria`. El actor es el `username` de `UsuarioAutenticado`.
- **Momento**: la fecha la pone el `Clock`.
- **Qué guarda**: la tabla guarda solo ids, la acción, el actor y la fecha. No hay columna de texto libre donde pueda filtrarse un DNI o un número de reserva.

*Alternativa descartada*: la columna `detalle jsonb` de la KB (`04_modelo_de_datos.md`). Se reemplaza por `turno_relacionado_id`, que es tipado y no admite datos sensibles. Si C-08 necesita el motivo de un sobreturno, lo agrega en su migración.

### D14. Migración V4
`V4__turnos_api.sql`:
1. `turno_estado_valido`: `DROP CONSTRAINT` y `ADD` con `RESERVADO`, `CONFIRMADO`, `CANCELADO`, `AUSENTE`, `ATENDIDO`. Además, `ALTER COLUMN estado SET DEFAULT 'RESERVADO'`.
2. `turno_sin_solapamiento_sillon` y `turno_sin_solapamiento_profesional`: `DROP` y `ADD` con las mismas columnas y `WHERE estado IN ('RESERVADO','CONFIRMADO')`. La de profesional mantiene `AND es_sobreturno = FALSE`. Los nombres no cambian, así D6 y los tests de C-03 siguen valiendo.
3. `codigo_reserva VARCHAR(12)`: se agrega nullable. Las filas existentes (solo datos de desarrollo) se completan con un valor único derivado de `md5`, después se pasa a `NOT NULL` y se agrega `UNIQUE` (`turno_codigo_reserva_key`). El formato lo valida el dominio, no la base, porque los valores de desarrollo no lo cumplen.

   *Ajuste durante el apply*: `RestriccionesTurnoIntegrationTest` (C-03) inserta turnos con SQL directo sin `codigo_reserva`, y con un `NOT NULL` sin valor por defecto sus tests fallarían. Por eso V4 crea la función `generar_codigo_reserva()`, con el mismo alfabeto de D3, y la usa para rellenar las filas existentes y como valor por defecto. Como así toda fila cumple el formato, se agrega además el `CHECK` `turno_codigo_reserva_formato`. Reemplaza la idea del relleno con `md5`, que podía traer `0` y `1`. La aplicación siempre envía su propio número, generado con `SecureRandom`.
4. `turno_auditoria`:
   - `id BIGINT GENERATED ALWAYS AS IDENTITY`;
   - `turno_id BIGINT NOT NULL REFERENCES turno`;
   - `accion VARCHAR(20) NOT NULL` con `CHECK` (`CREAR`, `CANCELAR`, `REPROGRAMAR`);
   - `actor VARCHAR(120) NOT NULL`;
   - `fecha TIMESTAMPTZ NOT NULL`;
   - `turno_relacionado_id BIGINT REFERENCES turno`;
   - índice por `turno_id`.

   El `CHECK` de acciones se amplía en las migraciones de C-08 y C-11.

**Entidades**: `TurnoEntity` (de Juan Cruz, con aviso) suma `codigoReserva`, el estado inicial `RESERVADO` y un método para cambiar el estado. Se crea `TurnoAuditoriaEntity` en `agenda/infrastructure`, porque es del módulo de agenda.

### D15. Niveles de test
- **Unitarios** (`./gradlew test`, sin base, con dobles en memoria): los escenarios de `agenda-turnos` (número de reserva, recursos inactivos, validación, cancelar y reprogramar).
- **Integración** (`@Tag("integration")`, PostgreSQL real): los escenarios de `agenda-api` y `auditoria-turnos`, más un test del adaptador. Ese test guarda directamente un turno que viola el `EXCLUDE`, para probar D6 sin depender de la carrera.
  - Usan una base `IntegracionAgendaBase` que extiende `IntegracionServidorBase` (de C-05, sin modificarla) y carga los datos ficticios con `JdbcTemplate`.
  - Reemplazan el `Clock` por uno fijo el 2026-12-01 a las 08:00.
- **Concurrencia**: el escenario "Dos reservas al mismo tiempo" se prueba con dos hilos y un `CountDownLatch`.

## Risks / Trade-offs

- **[Riesgo] Si un turno nuevo viola los dos `EXCLUDE` en una carrera, PostgreSQL informa el primero que evalúa, y puede no respetar la prioridad profesional > sillón.** → Solo pasa en carreras, porque fuera de ellas decide el dominio, que respeta la prioridad. Se documenta en el spec como "según el recurso que ya está ocupado".
- **[Riesgo] Si se cambia el margen, los turnos ya guardados conservan su `fin_con_margen` anterior.** → Es el comportamiento esperado (el margen rige para los turnos nuevos). C-04 lo tiene que tener en cuenta al hacerlo configurable.
- **[Riesgo] La traducción del `EXCLUDE` depende del nombre de las restricciones.** → V4 los conserva, y el test del adaptador falla si alguien los cambia.
- **[Trade-off] `buscarParaModificar` bloquea la fila (`PESSIMISTIC_WRITE`).** Dos cancelaciones simultáneas se serializan: la segunda ve `CANCELADO` y responde `TURNO_NO_ACTIVO`, en lugar de un error de concurrencia que el spec no define.
- **[Trade-off] `PrestacionRepository` desaparece, absorbido por `RecursosAgenda`.** Cambia una firma de C-01, pero es código propio y sin otros usuarios.
- **[Riesgo] El test de concurrencia puede no cruzar realmente los dos pedidos.** → El resultado esperado es el mismo en los dos casos (D6). La garantía de la base la prueba aparte el test del adaptador.
- **[Riesgo] Tocar `TurnoEntity`, que es de Juan Cruz.** → Se le avisa antes del PR (regla 3). Sus tests usan `CONFIRMADO` y `CANCELADO`, que siguen siendo válidos con V4.

## Migration Plan

1. V4 corre sola al arrancar, con Flyway. En las bases de desarrollo con turnos, el paso 3 les completa el `codigo_reserva`.
2. **Rollback**: no se edita V4 una vez mergeada. Si hay que revertir, se hace con una migración nueva, por la regla 3.
3. El frontend de C-09 empieza a usar los endpoints cuando este change esté en `master`.
