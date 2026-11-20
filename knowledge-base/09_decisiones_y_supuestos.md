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
**Decisión**: la regla se evalúa en el dominio (mensajes de error claros y tests por escenario) **y** en la base con `EXCLUDE USING gist (sillon_id WITH =, tstzrange(inicio, fin + interval '10 minutes') WITH &&) WHERE (estado IN ('RESERVADO','CONFIRMADO') AND NOT es_sobreturno)`, más la restricción equivalente por `profesional_id`.
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

## Supuestos inferidos

### SU-01: Los consultorios chicos sufren solapamientos y desorden de turnos
**Supuesto**: el problema existe y vale la pena resolverlo.
**Origen**: `discovery.md`, punto 10. El equipo no conoce consultorios de primera mano.
**Riesgo si es falso**: el producto resuelve un problema menor.
**Cómo validar**: entrevistar a 2 o 3 odontólogos o secretarias, con datos anonimizados.

### SU-02: Los pacientes prefieren reservar online antes que escribir por WhatsApp
**Origen**: `discovery.md`, punto 10.
**Riesgo si es falso**: baja adopción de la reserva online.
**Cómo validar**: medir el `origen` de los turnos una vez en uso.

### SU-03: Zona horaria única `America/Argentina/Buenos_Aires`
**Origen**: el mercado objetivo es Argentina.
**Riesgo si es falso**: errores de hora en el cálculo de las 24 h y los recordatorios.
**Cómo validar**: confirmarlo con el equipo (PA-09).

### SU-04: Tests de integración con PostgreSQL real (Testcontainers o una instancia local)
**Origen**: DD-03 exige `btree_gist`, que H2 no soporta.
**Riesgo si es falso**: si el equipo no tiene Docker, los tests de integración no corren en todas las máquinas.
**Cómo validar**: confirmar si los 5 integrantes tienen Docker (PA-11). Plan B: los tests de dominio son puros y corren siempre; los de integración se marcan con un tag aparte.

### SU-05: El frontend usa Vite como bundler de TypeScript vanilla
**Origen**: "TypeScript vanilla" no define cómo se compila; Vite tiene una plantilla `vanilla-ts` sin framework.
**Cómo validar**: confirmarlo con el equipo en el change de base del frontend.

### SU-06: Las duraciones de las prestaciones son múltiplos de 5 minutos
**Origen**: los ejemplos del Discovery (30 y 60 min) y el margen de 10 min.
**Riesgo si es falso**: rechazar duraciones válidas.
**Cómo validar**: confirmarlo con el equipo.
