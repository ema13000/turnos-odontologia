# turnos-odontologia

Sistema de gestión de turnos y agenda de pacientes para consultorios odontológicos (mercado argentino).
Trabajo Global de Integración de **Desarrollo de Software**: del Discovery al primer change, con Spec-Driven Development (OpenSpec) y Active Stack.

## Equipo

| Integrante | Rol |
|---|---|
| Sofia Indovino | Líder: fundación, ciclo OPSX, revisión de PR a `master` |
| Emanuel | Integrante |
| Juan Sebastián | Integrante |
| Juan Cruz | Integrante |
| Enzo | Integrante |

## Antes de empezar

1. Leé **[AGENTS.md](AGENTS.md)**: stack, skills y las reglas duras del proyecto.
2. Buscá tu change y su dueño en **[CHANGES.md](CHANGES.md)**. Trabajá en la rama `change/C-NN-<nombre>` y abrí un PR a `master`.

## Estado del proyecto

| Etapa | Estado |
|---|---|
| 0. Preparación del entorno | Hecho (`docs/evidencia/`) |
| 1. `/active-orchestrator` + `openspec init` | Hecho |
| 2. Discovery | Hecho: 5 fuentes verificadas por el equipo y [PDF](docs/discovery/informe-discovery.pdf) |
| 3. Base de conocimiento (`knowledge-base/`) | Hecho |
| 4. Roadmap (`CHANGES.md`) | Hecho: 32 changes (MVP C-01 a C-14) |
| 5. Skills y reglas del agente (`AGENTS.md`, `CLAUDE.md`) | Hecho: 5 skills en el repo, [registro](.atl/skill-registry.md) y 10 reglas duras |
| 6. Ciclo OPSX sobre un change | Hecho: C-01 `crear-turno-sin-solapamientos` (explore → propose → apply → verify → archive), 12 escenarios con su test. [Evidencia](docs/evidencia/etapa-6-c01-tests.txt) |
| 7. Memoria del agente (Engram) | Hecho: 13 memorias y consulta real ([evidencia y guion del video](docs/evidencia/etapa-7-engram.md)) |

El estado detallado de la fundación está en [`.active-orchestrator-state.json`](.active-orchestrator-state.json).

## Documentos principales

- [Instrucciones para agentes y reglas duras](AGENTS.md).
- [Roadmap de changes](CHANGES.md).
- [Base de conocimiento](knowledge-base/README.md).
- [Informe de Discovery](docs/discovery/informe-discovery.md) ([PDF](docs/discovery/informe-discovery.pdf)): 16 sistemas, matriz ponderada y MVP recomendado.
- [Verificación de fuentes del equipo](docs/discovery/verificacion/README.md).
- [Checklist de Discovery (11 puntos)](docs/discovery/discovery.md): problema, usuarios, reglas de negocio RN-01 a RN-09.
- [Evidencia por sistema](docs/discovery/sources/relevamiento-fuentes.md).

## Stack

- Backend: Spring Boot + Gradle + PostgreSQL
- Frontend: TypeScript vanilla, diseño responsivo

## Cómo trabajamos (Git)

1. Cada tarea o change se hace en su propia rama, que sale de `master` (por ejemplo, `verificacion/<nombre>` o `change/<nombre>`).
2. Cada integrante commitea con **su propio nombre y email de GitHub** (`git config user.name` / `git config user.email`).
3. Se abre un Pull Request a `master` y la líder lo revisa y lo mergea.
4. Nunca se suben datos reales de pacientes ni claves o credenciales.

## Instalación y tests

Requisito: Java 21. Si no lo tenés, Gradle lo descarga solo la primera vez. No hace falta instalar Gradle ni Docker para C-01.

```bash
cd backend
./gradlew test        # en Windows: gradlew.bat test
```

Corre los 14 tests del backend. Los 12 escenarios de `openspec/specs/agenda-turnos/spec.md` están en `CrearTurnoTest` y `MargenTest`, y cada test tiene el mismo nombre que su escenario. La guía completa de instalación (PostgreSQL y frontend) llega con C-14.
