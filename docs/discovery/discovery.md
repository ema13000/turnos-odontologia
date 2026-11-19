# Discovery: sistema de turnos para consultorios odontológicos

**Fecha**: 2026-11-19
**Conducido por**: Sofia Indovino (líder), con el agente (skill `discovery-research`)
**Fuentes investigadas**: 16 sistemas, sitios oficiales consultados el 2026-11-19 (ver `docs/discovery/sources/relevamiento-fuentes.md` y `docs/discovery/informe-discovery.md`)
**Equipo**: Sofia Indovino, Emanuel, Juan Sebastián, Juan Cruz, Enzo

> Convención: lo que dijo el equipo es una **decisión** o una **hipótesis**, y lo que salió de la investigación lleva su fuente. Los supuestos sin validar están marcados como tales.

## 1. Problema que resuelve

En los consultorios odontológicos chicos, los turnos llegan **desordenados por WhatsApp y por teléfono**, por lo que se pierden o se superponen, y **no se ve qué profesional y qué sillón están libres**. Las dos cosas hacen perder tiempo de recepción y generan turnos en conflicto.

*Validación*: la investigación muestra que el mercado resuelve la agenda multiprofesional, pero **solo 2 de 16 sistemas mencionan la agenda por sillón y ninguno documenta cómo evita los solapamientos entre profesional y sillón** (informe, C.3). Que los consultorios sufran este problema es **un supuesto**: el equipo no conoce consultorios de primera mano.

## 2. Usuarios / roles

- **Recepcionista o secretaria**: carga, mueve y cancela turnos, y carga sobreturnos.
- **Odontólogo**: ve su agenda, bloquea horarios y carga sobreturnos.
- **Administrador del consultorio** (puede ser el mismo odontólogo): configura profesionales, sillones y prestaciones con su duración.
- **Paciente**: reserva, confirma, cancela o reprograma su propio turno online.

Segmento: **odontólogo independiente** (1 profesional y 1 sillón, a veces con secretaria) y **consultorio chico** (varios profesionales que comparten sillones). Clínicas y cadenas multisucursal quedan fuera del alcance inicial.

## 3. Casos de uso

1. Como **recepcionista**, quiero crear un turno eligiendo paciente, profesional, sillón y prestación, para que el sistema me impida superponerlo con otro turno del mismo profesional o del mismo sillón.
2. Como **paciente**, quiero reservar un turno online según la disponibilidad real, para no tener que escribir ni llamar.
3. Como **paciente**, quiero cancelar o reprogramar mi turno hasta 24 h antes, para liberar el horario sin llamar.
4. Como **paciente**, quiero recibir un recordatorio por WhatsApp (o por email si no tengo WhatsApp) y confirmar asistencia, para no olvidarme del turno.
5. Como **odontólogo**, quiero ver mi agenda diaria y semanal y registrar el odontograma del paciente en su ficha.

El caso de uso **1** bloquea a todos los demás: es la base del MVP y el candidato al change OPSX.

## 4. Competidores / soluciones existentes

| Competidor | País | Puntaje (matriz B) | Pricing publicado | Diferenciador evidenciado |
|---|---|---|---|---|
| DentalCore | AR | 4,45 | ARS 75.000 o USD 50/mes (inconsistente) | Paquete local completo afirmado: WhatsApp API, ARCA, Mercado Pago, obras sociales, FHIR |
| DentalSoft | AR | 4,05 | Gratis (50 turnos) / $30.000 / $60.000 por profesional | Detección de solapamiento en sobreturnos; reserva del paciente en 4 pasos (demo) |
| AgendaPro | CL (página AR) | 3,85 | Desde ARS 13.900/mes | Agenda por sillón y duración por práctica (mockup); asistente IA multicanal |
| Bilog | AR | 3,35 | No publicado | Mayor base declarada en AR (+1.500 clínicas) |
| Dentalink | CL (módulo AR) | 3,30 | Cotización | Módulo de obras sociales argentinas (centro de ayuda) |
| Otros 11 | AR, LatAm e internacionales | 1,80 a 2,95 | Varía | Ver informe, sección A |

**Notas**: los cinco primeros son los recomendados para analizar en demo pública. Que el equipo no conozca consultorios hace que el estado actual (planilla, WhatsApp o papel) quede como supuesto.

## 5. Funcionalidades necesarias (v1)

Hipótesis del equipo, confirmada y completada con la investigación (informe, D.3):

1. Alta de profesionales, sillones o boxes y prestaciones con duración propia.
2. **Crear un turno sin solapamiento por profesional y por sillón** (vacío principal del mercado).
3. Cancelar y reprogramar turnos, liberando el horario.
4. Sobreturno, solo por personal del consultorio.
5. Agenda diaria y semanal por profesional y por sillón.
6. Reserva online por el paciente.
7. Recordatorio y confirmación por WhatsApp, o por email si el paciente no tiene WhatsApp.
8. Ficha básica del paciente con odontograma (estándar de mercado: 15 de 16 sistemas lo tienen).

## 6. Funcionalidades opcionales

- **v1.x (diferenciadores)**: lista de espera automática al liberarse un horario (0 de 16 sistemas la evidencian), política de cancelación configurable por consultorio, plan gratuito o precio publicado, y registro de consentimiento para comunicaciones.
- **Etapas posteriores**: obras sociales y prepagas (nomencladores, liquidaciones, validación), facturación electrónica ARCA, señas con Mercado Pago, periodontograma, imágenes y radiografías, consentimientos con firma electrónica, asistentes con IA, multisucursal y exportación HL7 FHIR.

## 7. Reglas de negocio

- **RN-01**: cada prestación tiene una **duración propia** (por ejemplo, consulta de 30 min o tratamiento de conducto de 60 min). El fin del turno se calcula como inicio más duración.
- **RN-02**: un **profesional** no puede tener dos turnos que se superpongan en el tiempo. Tampoco puede atender en dos sillones a la vez.
- **RN-03**: un **sillón o box** no puede tener dos turnos que se superpongan en el tiempo.
- **RN-04**: entre dos turnos consecutivos debe haber **10 minutos** libres, tanto **en el mismo sillón** (limpieza y preparación) como **para el mismo profesional** (aunque cambie de sillón). Ejemplo: si el Sillón 1 o la Dra. A tienen un turno de 10:00 a 10:30, el siguiente turno de ese sillón o de esa profesional puede empezar recién a las **10:40**. *(Decisión del equipo, 2026-11-19.)*
- **RN-05**: el solapamiento se evalúa con intervalos semiabiertos [inicio, fin). Dos turnos de **sillones y profesionales distintos** pueden ser contiguos o simultáneos. Si comparten sillón o profesional, se aplica RN-04, así que un turno que empieza justo cuando termina otro (10:30) **se rechaza**, y uno que empieza a las 10:40 **se acepta** (caso de borde).
- **RN-06**: solo el **personal** (recepcionista u odontólogo) puede cargar un **sobreturno**, que queda registrado como excepción a RN-02 y RN-03.
- **RN-07**: el **paciente** puede cancelar o reprogramar online hasta **24 h** antes del inicio del turno. Después de ese plazo, solo puede hacerlo el personal.
- **RN-08**: al cancelar un turno, el horario queda **libre** para otra reserva.
- **RN-09**: los recordatorios **no incluyen datos de salud**. Solo llevan nombre, fecha, hora, profesional y dirección. Se envían por WhatsApp o, si el paciente no tiene WhatsApp, **por email**.

## 8. Integraciones

- **v1**: WhatsApp Business API (API oficial de Meta) para recordatorios y confirmación. Tiene un costo de referencia de **USD 0,026 por mensaje** (dato publicado por DentalSoft), requiere plantillas aprobadas por Meta y un proveedor o cuenta de negocio. Se usa **email** como canal alternativo.
- **Posteriores**: Mercado Pago, ARCA (ex AFIP), webservices de obras sociales y Google Calendar.
- Para el trabajo práctico, las integraciones externas pueden **simularse** detrás de una interfaz (puerto o adaptador), sin cuentas reales.

## 9. Restricciones

- **Stack definido por el equipo**: backend en **Spring Boot + Gradle + PostgreSQL**; frontend en **TypeScript vanilla con diseño responsivo**.
- **Plazo**: entrega en **6 días** (alrededor del 2026-11-25), con 5 integrantes. Con eso solo es realista implementar completo el change OPSX y, en paralelo, changes chicos del roadmap.
- **Consigna**: el change no requiere interfaz gráfica; alcanza con una API o un módulo de dominio con tests por escenario.
- **Datos**: solo datos ficticios, nunca datos reales de pacientes.
- **Normativa argentina** aplicable a datos de salud: Ley 25.326 (datos personales, los de salud son sensibles) y Ley 26.529 (historia clínica). Hay que verificarlas en fuentes oficiales durante la KB.
- **Grupo de 5 integrantes**, cuando la consigna dice "hasta 3". Hay que confirmarlo con el docente.

## 10. Riesgos

- **Supuesto sin probar**: que los consultorios chicos sufren solapamientos y desorden de turnos. El equipo no lo validó con usuarios reales; solo se apoya en el vacío observado en el mercado.
- **Supuesto sin probar**: que los pacientes prefieren reservar online en lugar de escribir por WhatsApp.
- **Alcance contra plazo**: las cuatro áreas de la v1 no entran en 6 días. Mitigación: implementar el change OPSX y dejar el resto especificado en el roadmap.
- **Dependencia externa**: costo y aprobación de plantillas de WhatsApp Business API. Mitigación: adaptador simulado y email como respaldo.
- **Datos sensibles de salud**: la ficha y el odontograma exigen control de acceso y consentimiento. Mitigación: la ficha no entra en el change OPSX y los recordatorios no llevan datos de salud.
- **Calidad de la evidencia**: la mayoría de las funciones de los competidores son *afirmaciones comerciales*. Las cifras de impacto ("−90 % de ausencias") no tienen metodología publicada.
- **Paralelismo con 5 personas**: riesgo de conflictos de merge si dos changes tocan los mismos archivos. Mitigación: oleadas del roadmap y un dueño por módulo.

## 11. Preguntas abiertas

1. ~~Alcance del margen de 10 minutos~~ **Resuelta (2026-11-19)**: aplica al sillón **y** al profesional (ver RN-04 y RN-05).
2. ¿El margen de 10 minutos es fijo o configurable por consultorio o por prestación?
3. ¿Hace falta login para el paciente, o alcanza con DNI y teléfono o email para reservar?
4. ¿Qué pasa con los turnos de un paciente que confirma por WhatsApp pero no asiste (ausentismo)? ¿Se registra?
5. ¿El sobreturno debe exigir un motivo obligatorio?
6. Todo lo que el informe marcó como **"No evidenciado"** (por ejemplo, cómo resuelven los competidores la prevención de solapamientos por sillón y la lista de espera). Hay que confirmarlo en las demos públicas.
7. Si el docente acepta un grupo de 5 integrantes.
