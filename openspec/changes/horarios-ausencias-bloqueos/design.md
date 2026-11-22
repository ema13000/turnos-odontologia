# Design

## Context

- **`CrearTurno`** (C-01 y C-06, Sofia): valida en este orden:
  1. pasado;
  2. referencias y recursos inactivos;
  3. fin calculado;
  4. conflictos con margen (`Agenda.verificar`).

  Tiene tres entradas:
  - `preparar`: la usan crear y `ReprogramarTurno`.
  - `prepararExtension`: la usa `ExtenderTurno` (C-08).
  - Las dos terminan en `armar`.

  CHANGES.md pide que la líder revise cualquier cambio en `CrearTurno`.
- **`CrearSobreturno`** (C-08, Juan Cruz): tiene su propio camino. Verifica recursos, prueba sillones candidatos y solo relaja la restricción del profesional.
- **`AgendaConfig`**: arma los casos de uso sin Spring y publica un `ZoneId` (`app.zona-horaria`, por defecto `America/Argentina/Buenos_Aires`) y un `Clock`.
- **`TurnoRechazadoException(CodigoRechazo)`**: `AgendaExceptionHandler` la traduce a `409` con el código. Un valor nuevo del enum sale por la API sin tocar el manejador.
- **Tests**:
  - Los unitarios de la agenda construyen `CrearTurno` y `CrearSobreturno` directamente, en `AgendaEnMemoria`, `CrearTurnoTest`, `ExtenderTurnoTest` y `CrearSobreturnoTest`.
  - Todos los de integración que crean turnos por la API heredan de `IntegracionAgendaBase` (Sofia).
- **Seguridad**: hay sesión con roles, `@EnableMethodSecurity` y el principal `UsuarioAutenticado` con `profesionalId()`. C-04 ya resolvió permisos por rol con `@PreAuthorize` y `404 RECURSO_NO_ENCONTRADO` para profesionales inexistentes.
- **Tabla de dueños**: "`agenda/` reglas de horario y ausencias" es de Emanuel, con V5 reservada.

Motivación y alcance: ver `proposal.md`. Requisitos: ver `specs/`.

## Goals / Non-Goals

**Goals:**
- Que las reglas nuevas vivan en archivos propios, sin Spring, y que `CrearTurno` solo las llame en un punto.
- Que C-10 pueda reutilizar el mismo puerto y las mismas reglas para calcular la disponibilidad.
- Que los tests existentes de C-01, C-06 y C-08 no cambien de significado.

**Non-Goals:**
- Una garantía en la base para horarios, ausencias o bloqueos, como los `EXCLUDE` de los solapamientos. Estas reglas cambian con datos de configuración, y un turno ya dado no se invalida si después se registra una licencia (RN-AG-16 los lista).
- Calcular huecos libres (C-10).
- Excepciones por fecha en las franjas.

## Decisions

### D1. Puerto `DisponibilidadProfesional` y reglas puras
En `agenda/application` se agrega el puerto:

```java
interface DisponibilidadProfesional {
    PeriodosProfesional periodos(Long profesionalId, Intervalo ventana);
}
```

`PeriodosProfesional` es un record con:
- las franjas semanales del profesional;
- las ausencias que se cruzan con la ventana;
- los bloqueos que se cruzan con la ventana.

En `agenda/domain/reglas/` van, sin Spring:
- `FranjaHoraria`: día, desde y hasta, con validación propia.
- `HorarioSemanal`: el conjunto de franjas. Rechaza superposiciones y responde si un intervalo queda contenido en una franja, convirtiendo a hora local con un `ZoneId`.
- `PeriodoSinAtencion`: tipo `AUSENCIA`, `LICENCIA` o `BLOQUEO`, con inicio y fin.
- `ReglasDisponibilidad`: verifica un `Intervalo` con la prioridad de D3. Recibe `exigirHorario` para el sobreturno y la extensión.

*Alternativa descartada*: verificar directamente en el adaptador JPA. C-10 necesita evaluar miles de horarios candidatos en memoria, y las reglas puras se pueden probar sin base.

### D2. Integración en `CrearTurno` y `CrearSobreturno` con constructores compatibles
**`CrearTurno`** gana un constructor con `DisponibilidadProfesional` y `ZoneId`. El constructor actual se mantiene y delega con una disponibilidad "sin restricciones". Así los tests unitarios de C-01, C-06 y C-08 siguen compilando y significando lo mismo. Los escenarios nuevos usan el constructor nuevo. `armar` llama a las reglas después de verificar los recursos y antes de buscar conflictos:
- desde `preparar`, crear y reprogramar pasan `exigirHorario = true`;
- desde `prepararExtension`, la extensión pasa `false`.

**`CrearSobreturno`** gana un constructor análogo y llama a las reglas con `exigirHorario = false` después de verificar recursos y antes de probar sillones. Una ausencia rechaza el sobreturno sin importar el sillón.

**`AgendaConfig`** cablea los constructores nuevos con el adaptador real.

*Alternativa descartada*: un decorador alrededor de `CrearTurno`. La extensión y la reprogramación entran por métodos de paquete (`preparar`, `prepararExtension`) que un decorador no ve.

### D3. Prioridad y códigos
`CodigoRechazo` suma `PROFESIONAL_AUSENTE`, `AGENDA_BLOQUEADA` y `FUERA_DE_HORARIO`. El orden completo de validación queda así:
1. pasado;
2. referencias;
3. inactivos;
4. ausencia o licencia;
5. bloqueo;
6. fuera de horario;
7. solapamiento de profesional;
8. solapamiento de sillón;
9. margen.

Las ausencias y los bloqueos van antes que el horario porque dan información más útil: "está de licencia" explica por qué no hay atención, aunque el turno también caiga fuera de horario.
- **Cruce de períodos**: un turno `[inicio, fin)` se cruza con un período `[pi, pf)` si `inicio < pf && pi < fin`. Que un turno termine exactamente cuando empieza una ausencia no es un cruce.
- **El margen no se aplica contra ausencias ni bloqueos**: la limpieza del sillón no depende del profesional.

### D4. Franjas en hora local
- Las franjas se guardan como `dia_semana SMALLINT` (ISO: 1 = lunes) y `hora_desde`/`hora_hasta TIME`.
- Un turno está dentro de horario si, convertido a la zona de la agenda, su inicio y su fin caen el mismo día, ese día tiene una franja con `desde <= inicio` y `fin <= hasta`, y es la misma franja para los dos.
- Las franjas son semiabiertas: un turno puede terminar exactamente en `hasta`. Dos franjas contiguas (8–12 y 12–14) no se superponen, pero un turno que cruza las 12 está fuera de horario, porque ninguna franja lo contiene completo. Así lo pide RN-AG-13: "dentro de la misma franja".
- **Medianoche**: no hay franjas que la crucen. `hora_hasta` admite hasta las 23:59, y un consultorio chico no atiende de noche.

### D5. Ausencias y bloqueos en tablas separadas
La KB modela `AusenciaProfesional` (con tipo y registrado por) y `BloqueoAgenda` por separado, y tienen permisos distintos: la recepción gestiona ausencias, y el odontólogo y el administrador, bloqueos. Se respeta. Las dos tablas tienen:
- `profesional_id`, `inicio` y `fin` (`TIMESTAMPTZ`, con `CHECK fin > inicio`);
- `motivo VARCHAR(255)` nulo;
- `registrado_por` y `registrado_en`.

Un índice `(profesional_id, inicio, fin)` acelera la búsqueda por ventana.

Se permiten ausencias superpuestas del mismo profesional, por ejemplo una ausencia dentro de una licencia: no rompen ninguna regla y rechazarlas no aporta nada.

El `motivo` puede contener información sensible ("enfermedad"):
- solo lo ven quienes pueden leer el período;
- no aparece en la lista de turnos afectados ni en las respuestas de turnos;
- no va a logs.

### D6. Turnos afectados
Al registrar un período, el caso de uso consulta los turnos activos del profesional que se cruzan con él, con `TurnoRepository.buscarQueIntersecan`, pasándole el profesional y un sillón nulo, y filtra los activos. La respuesta `201` incluye el período creado y una lista con `turnoId`, `inicio`, `fin` y `prestacionId`, sin paciente.

Ya existe la API para reasignar y reprogramar (`POST /turnos/{id}/reprogramar` acepta otro profesional). C-07 no la duplica.

### D7. Horarios: reemplazo completo y doble garantía
- **El dominio valida primero**: `HorarioSemanal.de(franjas)` rechaza `HORARIO_INVALIDO` y `FRANJAS_SUPERPUESTAS` antes de tocar la base.
- **Reemplazo**: el `PUT` borra las franjas del profesional e inserta las nuevas en una transacción.
- **La base valida también**: un `EXCLUDE USING gist (profesional_id WITH =, dia_semana WITH =, tsrange(DATE '2000-01-03' + hora_desde, DATE '2000-01-03' + hora_hasta, '[)') WITH &&)` impide superposiciones aunque alguien inserte por SQL. `btree_gist` ya está instalada.

### D8. Paquetes y API
- **`agenda/domain/reglas/`**: las reglas de D1.
- **`agenda/disponibilidad/`**, en tres subpaquetes:
  - `application`: los casos de uso de horarios, ausencias y bloqueos.
  - `infrastructure`: entidades JPA propias, repositorios Spring Data y el adaptador `DisponibilidadProfesionalJpa`.
  - `api`: tres controladores y un `DisponibilidadExceptionHandler` limitado al paquete.
- **Rutas** bajo `/api/v1/profesionales/{id}/`:
  - `horarios`: `GET` y `PUT`.
  - `ausencias`: `GET` y `POST`, y `DELETE /ausencias/{ausenciaId}`.
  - `bloqueos`: `GET` y `POST`, y `DELETE /bloqueos/{bloqueoId}`.
- **Permisos**: `@PreAuthorize` por rol. Cuando el permiso depende de que sea "el propio profesional", el caso de uso compara `profesionalId()` de la sesión con el `{id}` de la ruta y lanza `AccessDeniedException`, que sale como `403 ACCESO_DENEGADO`.
- **Profesional inexistente**: `404 RECURSO_NO_ENCONTRADO`, con el mismo código que C-04.

### D9. Migraciones y seed
- **`V5__horarios_ausencias_bloqueos.sql`** crea `horario_trabajo`, `ausencia_profesional` y `bloqueo_agenda` con sus `CHECK`, el `EXCLUDE` de D7 y los índices.
- **`db/seed/V5_1__seed_horarios_dev.sql`** se carga solo en `dev`. Tiene los horarios de la KB: Dra. Ana Ejemplo de lunes a viernes de 8 a 13 y Dr. Bruno Prueba de lunes a viernes de 10 a 18. Busca a cada profesional por matrícula, como el seed `V2_1`.

### D10. Tests
- **Unitarios (sin base)**:
  - `FranjaHoraria`, `HorarioSemanal` y `ReglasDisponibilidad`: bordes de franja, días sin franja, franjas contiguas, cruce exacto con períodos y prioridad.
  - `CrearTurno` y `CrearSobreturno` con una `DisponibilidadProfesional` en memoria, para los escenarios de `agenda-turnos`.
- **Integración**:
  - `IntegracionAgendaBase` (Sofia) carga franjas de lunes a domingo de 00:00 a 23:59 para sus profesionales, para que los tests de C-06 y C-08 sigan probando lo mismo.
  - Una base propia de C-07 carga franjas puntuales por escenario.
  - Un test por escenario de los dos specs, con `@DisplayName` igual al nombre del escenario.

## Risks / Trade-offs

- **[Después del merge, una base sin horarios no acepta turnos]** → Es la regla elegida. En dev lo cubre el seed `V5_1`, y los tests cargan franjas en sus bases. Hay que avisar al equipo en el PR, sobre todo a Enzo (C-09) y a Juan Sebastián (C-10), que van a probar contra dev.
- **[El cambio en `CrearTurno` choca con trabajo en curso de Sofia o Juan Cruz]** → Es un solo punto de llamada y constructores nuevos, sin cambiar firmas existentes. Va en un commit separado para que Sofia lo revise aparte.
- **[Licencia registrada después de dar un turno]**: el turno queda dado. → RN-AG-16 lo resuelve: se lista y la recepción lo reasigna. Una extensión de ese turno sí se rechaza, porque se valida contra la ausencia.
- **[Consultas por cada turno creado]**: se suman una lectura de franjas y dos de períodos por turno. → Hay índices por profesional. El volumen de un consultorio chico es de decenas de turnos por día.
- **[Zona horaria]** → Se usa el `ZoneId` de `AgendaConfig`. Argentina no tiene horario de verano hoy. Si lo volviera a tener, la conversión a hora local con `ZoneId` lo resuelve.

## Migration Plan

1. V5 es aditiva: crea tablas nuevas y no modifica las existentes. El efecto sobre los turnos viene del código, no de la migración.
2. **Rollback**: revertir el commit de integración en `CrearTurno` desactiva las reglas sin tocar la base. Las tablas se pueden dejar o borrar con una migración nueva.

## Open Questions

- ¿C-10 va a necesitar consultar las franjas de todos los profesionales de una vez? El puerto se puede ampliar con un método por lista de profesionales sin cambiar estos specs.
