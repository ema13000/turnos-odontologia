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

## Tarea actual para el equipo: verificación de fuentes

Cada integrante verifica **una fuente** del informe de Discovery, a mano y sin IA.
Instrucciones, asignación y plantilla: **[docs/discovery/verificacion/README.md](docs/discovery/verificacion/README.md)**

## Estado del proyecto

| Etapa | Estado |
|---|---|
| 0. Preparación del entorno | Hecho (`docs/evidencia/`) |
| 1. `/active-orchestrator` + `openspec init` | Hecho |
| 2. Discovery | En curso: falta la verificación de fuentes y el PDF |
| 3. Base de conocimiento (`knowledge-base/`) | Pendiente |
| 4. Roadmap (`CHANGES.md`) | Pendiente |
| 5. Skills y reglas del agente (`AGENTS.md`, `CLAUDE.md`) | Pendiente |
| 6. Ciclo OPSX sobre un change | Pendiente |
| 7. Memoria del agente (Engram) | Pendiente |

El estado detallado de la fundación está en [`.active-orchestrator-state.json`](.active-orchestrator-state.json).

## Documentos principales

- [Informe de Discovery](docs/discovery/informe-discovery.md): 16 sistemas, matriz ponderada y MVP recomendado.
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

_Se completará en la Etapa 6, cuando exista el código del change._
