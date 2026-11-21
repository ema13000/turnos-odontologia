# Decisiones y Supuestos

## Decisiones documentadas

### DD-01: Stack: Spring Boot + Gradle + PostgreSQL y TypeScript vanilla
**Decisión**: backend en Spring Boot con Gradle y PostgreSQL; frontend en TypeScript sin framework, con diseño responsivo.
**Contexto**: el equipo lo definió en el Discovery (punto 9).
**Alternativas consideradas**: no se evaluaron otras; es una restricción del equipo.
**Justificación**: es el stack que maneja el equipo de 5 personas, y eso reduce la curva de aprendizaje con 6 días de plazo.
**Trade-offs aceptados**: sin framework de frontend hay más código manual de UI. Se mitiga porque la consigna no exige interfaz gráfica para el change.

### DD-02: El núcleo del MVP es "turno sin solapamiento por profesional y sillón, con margen"
**Decisión**: la primera funcionalidad, y el change OPSX, es crear un turno con la verificación de conflictos.
**Contexto**: el informe (C.3) muestra que ninguno de los 16 sistemas documenta la prevención de solapamientos entre profesional y sillón.
**Alternativas consideradas**: empezar por la reserva online o por los recordatorios (estándar de mercado).
**Justificación**: es el vacío del mercado, tiene reglas verificables (lo pide la consigna) y es base de todas las demás funcionalidades.
**Trade-offs aceptados**: el TP entrega un núcleo de dominio y no un producto completo.

### DD-03: Doble garantía contra solapamientos: dominio y restricción de exclusión en PostgreSQL
**Decisión**: la regla se evalúa en el dominio (mensajes de error claros y tests por escenario) **y** en la base con dos restricciones de exclusión:
- **por sillón**: `EXCLUDE USING gist (sillon_id WITH =, tstzrange(inicio, fin_con_margen) WITH &&) WHERE (estado IN ('RESERVADO','CONFIRMADO'))`. **Incluye los sobreturnos**, porque la limpieza del sillón es obligatoria (RN-AG-06);
- **por profesional**: la misma con `profesional_id`, agregando `AND NOT es_sobreturno`.

Como el margen es configurable (con mínimo 10), se guarda la columna `fin_con_margen = fin + margen vigente al crear el turno`.
**Contexto**: dos reservas concurrentes, una del personal y otra online, pueden pasar las dos la verificación en memoria.
**Alternativas consideradas**: bloqueo pesimista (`SELECT ... FOR UPDATE`) o aislamiento serializable.
**Justificación**: la restricción es declarativa, atómica y no depende de la disciplina del código.
**Trade-offs aceptados**: requiere la extensión `btree_gist`, y los tests de integración necesitan PostgreSQL real (ver SU-04). *Nota técnica: si cada fila guarda el rango `[inicio, fin + 10 min)`, dos filas A y B se intersecan exactamente cuando `A.inicio < B.fin + 10` y `B.inicio < A.fin + 10`, que es la definición de conflicto de `05_reglas_de_negocio.md`. Los casos 10:30 (rechaza) y 10:40 (acepta) se cubren con tests de integración.*

### DD-04: Integraciones externas detrás de puertos y simuladas en el TP
**Decisión**: WhatsApp y email se implementan como adaptadores de `NotificacionPort`; en el TP, son simulados (log o servidor SMTP de desarrollo).
**Contexto**: la WhatsApp Business API tiene costo (USD 0,026 por mensaje, dato de DentalSoft) y requiere aprobar plantillas. La consigna prohíbe crear cuentas con datos reales.
**Justificación**: permite probar el flujo sin cuentas ni costos.
**Trade-offs aceptados**: no se valida la entrega real del mensaje.

### DD-05: Un consultorio por despliegue (single-tenant) en v1
**Decisión**: no hay entidad `Consultorio` ni multi-tenencia en v1.
**Contexto**: el segmento es el independiente o el consultorio chico; multisucursal es "posterior" (informe, D.3).
**Trade-offs aceptados**: pasar a multi-tenant después requerirá una migración.

### DD-06: El sobreturno es una excepción explícita y auditada
**Decisión**: el sobreturno saltea RN-AG-02 a RN-AG-04, solo lo carga el personal y queda marcado y auditado.
**Contexto**: regla RN-06 del Discovery; todos los competidores que lo mencionan lo restringen al personal.

### DD-07: Privacidad desde el diseño en las comunicaciones
**Decisión**: los recordatorios no llevan datos de salud y requieren consentimiento.
**Contexto**: Ley 25.326 (los datos de salud son sensibles); el informe (C.3) muestra que ningún competidor dice qué datos viajan por WhatsApp.

### DD-08: Horarios de trabajo por profesional y reserva "con profesional" o "sin preferencia"
**Decisión**: cada profesional tiene franjas semanales de trabajo (RN-AG-13). El paciente elige un profesional o "sin preferencia". En ese caso ve la unión de los horarios libres y el sistema asigna el profesional y el sillón (RN-RO-04 a RN-RO-07).
**Contexto**: Ajustar de la líder sobre la KB v1 (2026-11-20). Sin horarios, el sistema ofrecía turnos en momentos en que nadie atiende, y la reserva online no definía qué sillón ni qué profesional le tocaba al paciente.
**Alternativas consideradas**: (a) usar solo bloqueos de agenda para cerrar las franjas sin atención; (b) que el paciente siempre elija profesional.
**Justificación**: (a) obliga a bloquear a mano cada semana; (b) le quita al paciente la opción de "el primer horario libre", que es lo más cómodo.
**Trade-offs aceptados**: suma una entidad (`HorarioTrabajo`) y un change al roadmap. **No amplía el change C-01** ("crear turno sin solapamientos"), que sigue enfocado en RN-AG-01 a RN-AG-05. La validación de horario se implementa en un change aparte que depende de C-01.

### DD-09: Respuestas de la líder a las preguntas abiertas (2026-11-20)
**Decisión**: se incorporan a las reglas las respuestas a PA-01 a PA-05, PA-07, PA-09, PA-12 y PA-14 a PA-16:
- el margen es configurable con un **mínimo de 10 min** (RN-AG-04);
- el **sobreturno** puede pisar la agenda del profesional, **nunca la limpieza del sillón** (RN-AG-06), y exige motivo;
- el paciente cancela o reprograma **desde su cuenta con el número de reserva**; por el consultorio, con **DNI y número de reserva** (RN-AG-07);
- se registran **ausencias y licencias del profesional** (RN-AG-16);
- la duración es **la misma para todos los profesionales**, con **extensión por complicaciones** (RN-AG-01, RN-AG-14);
- hay **especialidades** (RN-AG-15) y **sillón habitual** (RN-RO-06);
- en "sin preferencia" se asigna el profesional con **menos turnos** y, si hay empate, **al azar** (RN-RO-05).

**Contexto**: Ajustar de la líder sobre la KB v1.
**Trade-offs aceptados**: crece el modelo (cuentas de paciente, especialidades, ausencias). Nada de esto amplía el change C-01; cada punto va a su change del roadmap.

## Supuestos inferidos

### ~~SU-07: Todos los profesionales pueden hacer todas las prestaciones~~ (refutado)
**Refutado el 2026-11-20**: hay especialidades (DD-09, RN-AG-15).

### SU-08: La reserva online requiere cuenta de paciente
**Supuesto**: como el paciente cancela y reprograma "desde su cuenta", también **reserva** con la cuenta iniciada. No hay reserva anónima.
**Origen**: respuesta a PA-03.
**Riesgo si es falso**: se pierden reservas de pacientes que no quieren registrarse.
**Cómo validar**: PA-18.

### SU-01: Los consultorios chicos sufren solapamientos y desorden de turnos
**Supuesto**: el problema existe y vale la pena resolverlo.
**Origen**: `discovery.md`, punto 10. El equipo no conoce consultorios de primera mano.
**Riesgo si es falso**: el producto resuelve un problema menor.
**Cómo validar**: entrevistar a 2 o 3 odontólogos o secretarias, con datos anonimizados.

### SU-02: Los pacientes prefieren reservar online antes que escribir por WhatsApp
**Origen**: `discovery.md`, punto 10.
**Riesgo si es falso**: baja adopción de la reserva online.
**Cómo validar**: medir el `origen` de los turnos una vez en uso.

### SU-03: Zona horaria única `America/Argentina/Buenos_Aires` (**confirmado el 2026-11-20**)
**Origen**: el mercado objetivo es Argentina.
**Riesgo si es falso**: errores de hora en el cálculo de las 24 h y los recordatorios.
**Cómo validar**: confirmarlo con el equipo (PA-09).

### SU-04: Tests de integración con PostgreSQL real (Testcontainers o una instancia local)
**Origen**: DD-03 exige `btree_gist`, que H2 no soporta.
**Riesgo si es falso**: si el equipo no tiene Docker, los tests de integración no corren en todas las máquinas.
**Cómo validar**: confirmar si los 5 integrantes tienen Docker (PA-11). La líder sí lo tiene.
**Alternativas si alguien no tiene Docker**:
1. **Tests de dominio puros** (sin base): cubren **todos los escenarios del spec** y corren en cualquier máquina con Java. Son los que exige la consigna.
2. **PostgreSQL instalado localmente** (instalador oficial para Windows): los tests de integración apuntan a esa base con variables de entorno.
3. Tests de integración con un **tag aparte** (`@Tag("integration")`): quien no tenga base los saltea, y los corre al menos la líder antes de cada merge.

### SU-05: El frontend usa Vite como bundler de TypeScript vanilla
**Origen**: "TypeScript vanilla" no define cómo se compila; Vite tiene una plantilla `vanilla-ts` sin framework.
**Cómo validar**: confirmarlo con el equipo en el change de base del frontend.

### SU-06: Las duraciones de las prestaciones son múltiplos de 5 minutos
**Origen**: los ejemplos del Discovery (30 y 60 min) y el margen de 10 min.
**Riesgo si es falso**: rechazar duraciones válidas.
**Cómo validar**: confirmarlo con el equipo.
