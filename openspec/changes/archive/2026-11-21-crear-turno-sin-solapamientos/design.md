# Design

## Context

- No hay código todavía: este change crea `backend/` desde cero. Ver `proposal.md` (Why) para la motivación y `specs/agenda-turnos/spec.md` para los requisitos.
- La arquitectura acordada es hexagonal (`knowledge-base/08_arquitectura_propuesta.md`): el dominio de agenda no conoce la base ni la web, para que cada escenario del spec se pruebe sin infraestructura.
- Los 5 integrantes van a construir sobre este scaffolding (C-02 a C-14), así que el build tiene que funcionar en cualquier máquina con Java, sin Docker.
- En la máquina de la líder, `java` por defecto es JDK 25 y hay un JDK 21 instalado aparte. Gradle no está instalado globalmente, pero hay distribuciones en la caché del wrapper.

## Goals / Non-Goals

**Goals:**
- Un dominio de agenda testeable en milisegundos, sin Spring en las clases de dominio.
- Un build reproducible: `./gradlew test` en verde con Java 21, también en las máquinas del equipo.
- Trazabilidad 1 a 1 entre cada escenario del spec y su test.

**Non-Goals:**
- Adaptadores reales (JPA, REST): este change solo define los puertos.
- Concurrencia: dos reservas simultáneas en el mismo hueco se resuelven con la restricción de exclusión de PostgreSQL (DD-03) en C-03 y C-06. Este change no la simula.
- Validar la existencia o el estado de pacientes, profesionales y sillones (RN-AG-11): C-04 y C-07.

## Decisions

### D1. Estructura de paquetes hexagonal

```
backend/src/main/java/ar/turnos/odontologia/
  TurnosOdontologiaApplication.java
  agenda/
    domain/        Intervalo, Turno, EstadoTurno, Margen, Agenda,
                   CodigoRechazo, TurnoRechazadoException
    application/   CrearTurno, CrearTurnoCommand,
                   TurnoRepository (puerto), PrestacionRepository (puerto)
backend/src/test/java/ar/turnos/odontologia/agenda/
    domain/        MargenTest
    application/   CrearTurnoTest, TurnoRepositoryEnMemoria, PrestacionRepositoryEnMemoria
```

Las implementaciones en memoria viven en `src/test`: son dobles de prueba, no código de producción. C-03 y C-06 agregan `agenda/infrastructure` y `agenda/api`.

*Alternativa descartada*: paquetes por capa técnica (`controllers/`, `services/`). Mezcla módulos de distintos dueños y choca con la tabla de dueños de `CHANGES.md`.

### D2. Tiempo como `Instant` y `Clock` inyectado

- El dominio usa `Instant` (instante absoluto). Así se mapea directo a `TIMESTAMPTZ` en C-03 y no hay ambigüedad con los cambios de hora.
- La zona `America/Argentina/Buenos_Aires` se usa solo en los bordes: los tests arman los instantes con `ZonedDateTime` en esa zona.
- `CrearTurno` recibe un `java.time.Clock`. Los tests usan `Clock.fixed(...)`, así el escenario "inicio igual al momento actual" es determinista.

*Alternativa descartada*: `LocalDateTime`. No guarda la zona, y en C-03 habría que suponerla.

### D3. La regla de conflicto vive en `Agenda`, con prioridad fija

`Agenda` recibe el turno nuevo, los turnos existentes y el `Margen`, y devuelve el rechazo de mayor prioridad, si hay alguno:

1. Descarta los turnos no activos (solo cuentan `RESERVADO` y `CONFIRMADO`).
2. Para cada turno que comparte **profesional**: si los intervalos se superponen (`N.inicio < E.fin` y `E.inicio < N.fin`), corresponde `SOLAPAMIENTO_PROFESIONAL`.
3. Para cada turno que comparte **sillón**: si los intervalos se superponen, corresponde `SOLAPAMIENTO_SILLON`.
4. Para cada turno que comparte **algún** recurso: si no hay superposición, pero no se cumple el margen (`N.inicio < E.fin + M` y `E.inicio < N.fin + M`), corresponde `MARGEN_INSUFICIENTE`.
5. Se informa el código de mayor prioridad encontrado: profesional > sillón > margen.

El filtro de estados vive en el dominio, no en el repositorio. Así el escenario "un turno cancelado no bloquea" prueba la regla, no un detalle de la consulta.

*Alternativa descartada*: frenar en el primer conflicto que aparece. El resultado dependería del orden de la lista y el escenario de prioridad no sería determinista.

### D4. Un solo tipo de excepción con código estable

`TurnoRechazadoException` lleva un `CodigoRechazo` (enum): `SOLAPAMIENTO_PROFESIONAL`, `SOLAPAMIENTO_SILLON`, `MARGEN_INSUFICIENTE`, `INICIO_EN_EL_PASADO` y `MARGEN_INVALIDO`. Los tests verifican el código, no el mensaje. En C-06, la API traduce el código a `409` o `422` con `ProblemDetail` (regla dura 9).

*Alternativa descartada*: una clase de excepción por error, como sugería la primera versión de `CHANGES.md`. Multiplica clases sin agregar comportamiento, y la API igual necesita el código.

### D5. `Margen` como objeto de valor

`Margen.deMinutos(int)` rechaza valores menores a 10 con `MARGEN_INVALIDO`, y `Margen.PREDETERMINADO` vale 10. `CrearTurno` recibe el margen vigente en su construcción. En C-04, la configuración del consultorio va a proveer ese valor.

### D6. Puertos mínimos

- `PrestacionRepository.duracionDe(prestacionId)`: devuelve la duración (`Duration`) de la prestación. Si no existe, lanza `IllegalArgumentException`: es un error de programación en este change, y C-04 le asigna un código.
- `TurnoRepository.buscarQueIntersecan(profesionalId, sillonId, desde, hasta)`: devuelve los turnos del profesional **o** del sillón que se cruzan con `[desde, hasta)`, en cualquier estado. `CrearTurno` consulta `[inicio − M, fin + M)`, porque un turno existente puede generar conflicto con margen solo si se cruza con esa ventana. Esto reemplaza la ventana con "duración máxima" del Flujo 1 de la KB.
- `TurnoRepository.guardar(turno)`.

Los identificadores son `UUID`, como en `knowledge-base/04_modelo_de_datos.md`.

### D7. Orden de validación en `CrearTurno`

Primero el pasado (`INICIO_EN_EL_PASADO`), después el cálculo del fin y por último los conflictos. Un turno en el pasado se rechaza sin consultar la agenda.

### D8. Build: Gradle wrapper, toolchain de Java 21 y Spring Boot 3.5

- **Gradle wrapper** commiteado, con una versión 9.x compatible con el plugin de Spring Boot. La versión exacta se fija y se verifica en la tarea 1.
- **Toolchain de Java 21** en `build.gradle`, con el plugin `foojay-resolver-convention` en `settings.gradle`. Si un integrante no tiene JDK 21, Gradle lo descarga solo. Esto evita el problema de la líder, que tiene JDK 25 como `java` por defecto.
- **Spring Boot 3.5.x** con el starter base y `spring-boot-starter-test`, que ya trae JUnit 5, AssertJ y Mockito. Se suma una clase de aplicación vacía, para que C-03 y C-06 agreguen los starters web y de datos sin rehacer el build.
- **Tests de integración**: C-01 no tiene ninguno. Cuando C-03 los agregue, van con `@Tag("integration")` (regla dura 7).

*Alternativa descartada*: un build sin Spring Boot. Habría que migrarlo en C-03, y todos los integrantes trabajarían esos días contra un build que cambia.

### D9. Trazabilidad de escenarios a tests

Cada escenario del spec tiene un método de test con `@DisplayName` igual al nombre del escenario:

| Escenario | Clase de test |
|---|---|
| Turno sin conflictos | `CrearTurnoTest` |
| Un turno cancelado no bloquea el horario | `CrearTurnoTest` |
| El profesional ya atiende en otro sillón | `CrearTurnoTest` |
| El sillón ya está ocupado por otro profesional | `CrearTurnoTest` |
| Turno contiguo sin margen | `CrearTurnoTest` |
| Turno que respeta el margen exacto | `CrearTurnoTest` |
| Turno contiguo sin recursos compartidos | `CrearTurnoTest` |
| Margen menor al mínimo | `MargenTest` |
| Margen mayor al mínimo | `CrearTurnoTest` |
| Choque por profesional y por sillón a la vez | `CrearTurnoTest` |
| Inicio anterior al momento actual | `CrearTurnoTest` |
| Inicio igual al momento actual | `CrearTurnoTest` |

Los tests de `CrearTurnoTest` entran por el caso de uso, con los dobles en memoria. Así prueban el comportamiento del spec y no detalles internos.

## Risks / Trade-offs

- **[Riesgo] El margen vigente contra el margen guardado.** El dominio compara con el margen vigente al crear el turno, pero DD-03 guarda en la base `fin_con_margen` con el margen de cada turno existente. Si el administrador sube el margen, el dominio y la base podrían no coincidir para los turnos viejos. → Mitigación: lo anoto para C-03, que debe decidir si la restricción de exclusión usa el margen guardado o si se recalcula.
- **[Riesgo] Versiones de Gradle y Spring Boot incompatibles.** → Mitigación: la tarea 1 se cierra recién con `./gradlew test` en verde, y se mergea sola, para que nadie arranque sobre un build roto.
- **[Riesgo] Sin descarga de JDK.** Si un integrante no tiene red para que foojay descargue Java 21, el build falla. → Mitigación: el README de C-14 documenta cómo instalar JDK 21 a mano.
- **[Trade-off] Superposición calculada de a pares.** `Agenda` recorre los turnos existentes uno por uno. Alcanza con las cantidades de un consultorio chico, porque el repositorio ya filtra por ventana.

## Migration Plan

No aplica: no hay código, datos ni despliegues previos. Para volver atrás, alcanza con revertir el PR.
