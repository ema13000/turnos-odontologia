# Preguntas Abiertas

## Inconsistencias detectadas

### IN-01: Turno contiguo: ¿se solapa o no? *(resuelta)*
**Documento A dice**: la primera versión del Discovery decía que "un turno que empieza justo cuando termina otro no se considera solapado".
**Documento B dice**: la regla RN-04 (agregada el 2026-11-19) exige 10 minutos de margen entre turnos que comparten sillón o profesional.
**Impacto**: definía el escenario de borde del spec.
**Resolución**: el equipo decidió el 2026-11-19 que el margen aplica al sillón **y** al profesional. Contiguos que comparten recurso se rechazan; con distintos recursos, se aceptan (RN-AG-05).

### IN-02: Precio de DentalCore
**Documento A dice**: la home publica ARS 75.000/mes.
**Documento B dice**: el FAQ publica USD 50/mes.
**Impacto**: el puntaje de precio en la matriz B y el posicionamiento comercial.
**Resolución propuesta**: verificarlo a mano (fuente opcional 6 del informe). No afecta al MVP.

### IN-03: Portal del paciente de VecDent *(resuelta)*
**Documento A dice**: el relevamiento automático marcaba "Comprobado (portal en demo)".
**Documento B dice**: la verificación manual de Sofia (2026-11-20) encontró que solo el personal crea turnos.
**Resolución**: el informe se corrigió a "No evidenciado". La lección se incorporó a RN-RO-01 y a la matriz RBAC: el paciente no es un rol del personal.

## Preguntas abiertas (priorizadas)

| ID | Prioridad | Pregunta | Bloquea | Decisor |
|---|---|---|---|---|
| PA-01 | **Alta** | ¿El margen de 10 min es **fijo** o configurable por consultorio o por prestación? (Por ahora es configurable por variable de entorno, con valor 10.) | Spec del change OPSX | Equipo (líder) |
| PA-02 | **Alta** | ¿Un **sobreturno** también saltea el margen de 10 min, o solo el solapamiento? | Spec de sobreturno | Equipo |
| PA-03 | **Alta** | ¿El paciente necesita **login**, o alcanza con DNI y un código o token de reserva para cancelar o reprogramar? | Reserva online | Equipo |
| PA-04 | Media | ¿Se registra el **ausentismo** (estado `AUSENTE`) y quién lo marca? | Métricas | Equipo |
| PA-05 | Media | ¿El **sobreturno** exige un **motivo** obligatorio? | Sobreturno | Equipo |
| PA-06 | Media | ¿Hay **horario de atención** del consultorio o de cada profesional? ¿Se rechazan los turnos fuera de horario? | Disponibilidad | Equipo |
| PA-07 | Media | ¿La duración de una prestación puede **variar según el profesional**? | Modelo de datos | Equipo |
| PA-08 | ~~Media~~ | ~~¿Con cuánta anticipación se envía el recordatorio?~~ **Resuelta (2026-11-20)**: 24 h antes (RN-NO-04). | — | Equipo |
| PA-09 | Baja | ¿Se confirma la zona horaria única `America/Argentina/Buenos_Aires`? (SU-03) | Fechas | Equipo |
| PA-10 | Media | Autenticación del personal: ¿sesión o **JWT**? | Change de base | Equipo técnico |
| PA-11 | Media | ¿Los 5 integrantes tienen **Docker** para los tests de integración con PostgreSQL? (SU-04) | Verify y README | Equipo técnico |
| PA-12 | Baja | ¿El docente acepta un grupo de **5 integrantes**? La consigna dice "hasta 3". | Entrega | Líder con el docente |
| PA-13 | Baja | Verificaciones pendientes de **Bilog** (Emanuel) y **DentalSoft** (Juan Sebastián): si encuentran errores, se aplica *Ajustar* al informe y a esta KB. | Cierre del Discovery | Emanuel, Juan Sebastián |

## Puntos "No evidenciado" del Discovery

Información que el relevamiento **no pudo respaldar con fuentes públicas** (informe, secciones A y C.3). No se completa por deducción; se confirma en las demos públicas o queda abierta.

| # | Qué no está evidenciado | En qué sistemas | Por qué importa |
|---|---|---|---|
| NE-01 | **Cómo previenen los solapamientos por sillón**, y si lo hacen | Todos los relevados. Solo AgendaPro y Órbita mencionan la agenda por sillón, sin reglas | Es el diferencial del MVP (DD-02); confirmar que el vacío es real |
| NE-02 | **Lista de espera** | Los 16 sistemas | Diferenciador v1.x (US-100) |
| NE-03 | **Prevención de solapamientos por profesional** | Todos menos DentalSoft (detección solo en sobreturnos) | Validar el diferencial |
| NE-04 | **Precios** de Bilog, DentalTec, Dentalink, Órbita, Doctocliq, Dentidesk, Clinic Cloud, Medesk y Open Dental | 9 de 16 | Posicionamiento de precio |
| NE-05 | **Qué datos viajan en los recordatorios** por WhatsApp | Los 16 sistemas | Justifica RN-NO-01 |
| NE-06 | **Normativa argentina declarada** (Leyes 25.326 y 26.529) | 13 de 16 (solo DentalCore, AgendaPro y Bilog la citan) | Requisito legal propio (DD-07) |
| NE-07 | **Reserva online por el propio paciente** | DentalTec, DentalCore, Órbita, Doctocliq, VecDent (corregido tras la verificación) | El estándar de mercado es menos uniforme de lo que parecía |
| NE-08 | **ARCA y Mercado Pago** | Todos menos DentalCore (ambos), DentalTec (ARCA), DentalSoft y AgendaPro (MP) | Etapas posteriores |
| NE-09 | **Odontograma en Clinic Cloud**; **licencia y español de Open Dental** | Esos sistemas | Referencias internacionales |
| NE-10 | **Moneda de los precios** publicados con "$" | DentalSoft, VecDent | Comparación de precios |
