# Etapa 7: memoria del agente (Engram)

Engram es la memoria persistente del agente. Guarda en una base local (`~/.engram/engram.db`) las decisiones, los hallazgos y las convenciones del proyecto, y el agente los recupera en sesiones nuevas sin que el equipo tenga que volver a explicar el contexto.

- **Proyecto en Engram**: `turnos-odontologia`
- **Versión**: engram v3.7.2 (instalado con Active Stack)
- **Estado al 2026-11-21**: 13 memorias, 1 resumen de sesión y 0 conflictos pendientes

## 1. Qué quedó guardado

Cada memoria tiene un **tipo**, un **tema** (`topic_key`) y un contenido con el formato *Qué / Por qué / Dónde / Qué aprendimos*. Si se guarda de nuevo con el mismo tema, la memoria se actualiza en lugar de duplicarse.

### Decisiones

| # | Tema | Memoria |
|---|---|---|
| 5 | `foundation/plan` | Plan de fundación aprobado por la líder y `openspec init` |
| 6 | `discovery/respuestas-checklist` | Stack, alcance v1 y reglas de agenda definidos en el Discovery |
| 9 | `architecture/kb-turnos` | Respuestas de la líder a las preguntas abiertas de la KB (margen configurable con mínimo de 10, sobreturno, sesión con cookie…) |
| 10 | `roadmap/changes` | Roadmap ampliado a 32 changes (MVP y post-MVP) |
| 13 | `agents/reglas-duras` | AGENTS.md/CLAUDE.md con las 10 reglas duras confirmadas y las 2 rechazadas |
| 14 | `changes/c-01-crear-turno` | Ciclo OPSX de C-01 completo (PR #6 y #7) |
| 15 | `agenda/reglas-conflicto` | Reglas de conflicto decididas en el explore de C-01 (prioridad de errores, turnos activos, margen) |

### Hallazgos

| # | Tema | Memoria |
|---|---|---|
| 7 | `discovery/informe-hallazgos` | Hallazgos clave del informe de Discovery (16 sistemas) |
| 12 | `discovery/verificacion-fuentes` | Verificación manual de las 5 fuentes: VecDent tenía un dato falso y DentalSoft tenía precios mal leídos |

### Convenciones y preferencias

| # | Tema | Memoria |
|---|---|---|
| 8 | `equipo/asignacion` | C-01 lo hace la líder; avisar cuando el siguiente paso es de un compañero |
| 16 | `equipo/convenciones` | Ramas `change/C-NN`, PR revisado por la líder, un dueño por módulo, migraciones reservadas, orden de merge |
| 11 | `skill-registry` | Skills del proyecto y por qué se copiaron en lugar de usar symlinks |

### Resumen de sesión

| # | Memoria |
|---|---|
| 17 | Resumen de la sesión de fundación: objetivo, hallazgos, qué se logró, próximos pasos y archivos relevantes |

## 2. Consulta real

Consulta desde la terminal, con la palabra clave "margen":

```text
$ engram search "margen" --project turnos-odontologia --limit 5
Found 3 memories:

[1] #15 (decision) — Conflict rules for turnos decided in C-01 explore
    1. If a turn conflicts on both professional and chair, return SOLAPAMIENTO_PROFESIONAL.
       The full priority is SOLAPAMIENTO_PROFESIONAL > SOLAPAMIENTO_SILLON > MARGEN_...
    2026-11-21 13:49:53 | project: turnos-odontologia

[2] #9 (decision) — Respuestas de la líder a preguntas abiertas de la KB
    Learned: Margen configurable mínimo 1...
    2026-11-20 20:38:56 | project: turnos-odontologia

[3] #6 (decision) — Definido stack, alcance v1 y reglas de agenda en Discovery
    2026-11-19 00:38:36 | project: turnos-odontologia
```

La consulta devuelve, ordenadas por relevancia, decisiones tomadas en tres días distintos (19, 20 y 21 de noviembre). Esos días son tres momentos del proyecto: Discovery, KB y explore de C-01.

## 3. Cómo evita que el agente arranque de cero

| Sin Engram | Con Engram |
|---|---|
| Cada sesión nueva empieza sin contexto: hay que volver a explicar el stack, el equipo y qué se decidió. | Al empezar, el agente llama a `mem_context` y recibe las últimas decisiones y el resumen de la sesión anterior. |
| El agente puede contradecir una decisión ya tomada (por ejemplo, proponer JWT cuando se eligió sesión con cookie). | El agente busca la decisión (`mem_search "cookie"`) antes de proponer, y respeta lo acordado. |
| Lo que se aprendió de un error se pierde: VecDent, DentalSoft, los symlinks en Windows. | Los hallazgos quedan guardados con su causa, y el agente no repite el error. |
| Las decisiones de las conversaciones (por ejemplo, "si choca por los dos recursos, gana el profesional") viven solo en el chat. | Quedan en una memoria con tema propio, enlazadas al spec y al código que las implementa. |

Engram **no reemplaza** a los archivos del repo: la KB, `CHANGES.md` y los specs siguen siendo la fuente de verdad. Engram guarda **por qué** se decidió algo y **dónde** está, para que el agente sepa qué leer.

## 4. Guion para el video

1. **Mostrar lo guardado.** En una terminal, en la carpeta del repo:
   ```powershell
   engram stats --project turnos-odontologia
   ```
   ```powershell
   engram tui
   ```
   En la TUI se recorren las memorias por tipo. Mostrar una decisión (#15), un hallazgo (#12) y una convención (#16).
2. **Abrir una sesión nueva de Claude Code** en la carpeta del repo (chat nuevo, sin historial) y escribir:
   > ¿Qué decidimos sobre qué error devolver cuando un turno choca por profesional y por sillón a la vez, y por qué?

   El agente debería consultar Engram (`mem_search`) y responder que gana `SOLAPAMIENTO_PROFESIONAL`, con la prioridad completa, citando la memoria #15 y el spec.
3. **Segunda pregunta** en la misma sesión nueva:
   > ¿Qué encontramos al verificar las fuentes del Discovery?

   Respuesta esperada: el portal del paciente de VecDent era falso y los precios de DentalSoft estaban mal leídos (memoria #12).
4. **Explicar** con la tabla de la sección 3 por qué eso evita arrancar de cero.

## 5. Lecciones sobre el uso de Engram

- **Un mismo tema pisa la memoria anterior.** Las tres memorias de C-01 (spec, implementación y cierre) usaban `changes/c-01-crear-turno`, y cada una reemplazó a la anterior. Así se perdieron las decisiones del explore. Se recuperaron en una memoria con tema propio (#15). Conclusión: un tema por asunto, no por change.
- **Las búsquedas con muchas palabras no encuentran nada**, porque exigen que estén todas. `mem_search "margen solapamiento prioridad error sillón profesional"` no devolvió resultados, y `"margen"` devolvió tres. Conviene buscar con una o dos palabras clave.
- **Conflictos entre memorias.** Al guardar, Engram detecta memorias parecidas y pide un veredicto (`related`, `supersedes`, `conflicts_with`). Todos los casos de este proyecto se marcaron como `related`.
