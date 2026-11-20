# turnos-odontologia: Base de Conocimiento

Base de conocimiento generada por `kb-creator` (modo import) el 2026-11-20 a partir del Discovery: `docs/discovery/discovery.md`, `docs/discovery/informe-discovery.md` y `docs/discovery/sources/relevamiento-fuentes.md`.

## Índice de archivos

| Archivo | Contenido |
|---|---|
| [01_vision_y_objetivos.md](01_vision_y_objetivos.md) | Propósito, objetivos por actor, alcance del MVP (sección D del informe), fuera de alcance |
| [02_descripcion_general.md](02_descripcion_general.md) | Stack (Spring Boot, Gradle, PostgreSQL, TS vanilla), arquitectura, integraciones, API REST |
| [03_actores_y_roles.md](03_actores_y_roles.md) | Recepcionista, odontólogo, administrador, paciente; matriz RBAC; rutas públicas |
| [04_modelo_de_datos.md](04_modelo_de_datos.md) | Entidades (Turno, Profesional, Sillón, Prestación…), ERD, restricciones, seed ficticio |
| [05_reglas_de_negocio.md](05_reglas_de_negocio.md) | RN-AG (agenda y solapamientos con margen de 10 min), RN-RO, RN-NO, RN-CL, RN-GL |
| [06_funcionalidades.md](06_funcionalidades.md) | Épicas e historias US-001 a US-041; backlog v1.x y posterior |
| [07_flujos_principales.md](07_flujos_principales.md) | Crear turno, reserva online, cancelar o reprogramar, recordatorio y confirmación |
| [08_arquitectura_propuesta.md](08_arquitectura_propuesta.md) | Hexagonal, estructura de directorios, seguridad, variables de entorno |
| [09_decisiones_y_supuestos.md](09_decisiones_y_supuestos.md) | DD-01 a DD-07, SU-01 a SU-06 |
| [10_preguntas_abiertas.md](10_preguntas_abiertas.md) | Inconsistencias, preguntas priorizadas y puntos "No evidenciado" del Discovery |

## Quick Start para desarrolladores

1. Entender el dominio → [01](01_vision_y_objetivos.md), [03](03_actores_y_roles.md)
2. Entender los datos → [04](04_modelo_de_datos.md)
3. Entender las reglas → [05](05_reglas_de_negocio.md) (**RN-AG-01 a RN-AG-05 son el corazón del sistema**)
4. Entender la arquitectura → [02](02_descripcion_general.md), [08](08_arquitectura_propuesta.md)
5. Implementar → [07](07_flujos_principales.md), [06](06_funcionalidades.md)
6. Antes de codificar → [10](10_preguntas_abiertas.md) (PA-01 a PA-03 son de prioridad alta)

## Resumen ejecutivo

Sistema de agenda para consultorios odontológicos chicos de Argentina. Su diferencial es **impedir que se superpongan los turnos por profesional y por sillón, con 10 minutos de margen**, un vacío que no documenta ninguno de los 16 competidores relevados. El MVP suma reserva online, recordatorios por WhatsApp o email sin datos de salud, y una ficha con odontograma. El trabajo práctico implementa el núcleo (US-010) con el ciclo OPSX; el resto queda en el roadmap.
