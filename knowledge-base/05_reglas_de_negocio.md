# Reglas de Negocio

Cada regla tiene un código `RN-{DOMINIO}-{NN}`. Entre paréntesis va el código original del Discovery (`docs/discovery/discovery.md`, punto 7) para mantener la trazabilidad.

## Definiciones

- **Intervalo de un turno**: `[inicio, fin)`, semiabierto, con `fin = inicio + duración de la prestación`.
- **Margen**: 10 minutos (`MARGEN = 10 min`).
- **Recurso**: el profesional o el sillón de un turno.
- **Turno activo**: un turno en estado `RESERVADO` o `CONFIRMADO`. Los `CANCELADO` no ocupan agenda. La consideración de `ATENDIDO` y `AUSENTE` es irrelevante para turnos futuros.
- **Conflicto**: un turno nuevo `N` está en conflicto con un turno activo existente `E` que comparte **profesional o sillón** si
  `N.inicio < E.fin + MARGEN` **y** `E.inicio < N.fin + MARGEN`.

## Dominio: Agenda (RN-AG)

- **RN-AG-01** (RN-01): cada prestación tiene una **duración propia** en minutos. El fin del turno se calcula como `inicio + duración`; no se ingresa a mano.
- **RN-AG-02** (RN-02): un **profesional** no puede tener dos turnos activos en conflicto. En particular, **no puede atender en dos sillones a la vez**.
- **RN-AG-03** (RN-03): un **sillón o box** no puede tener dos turnos activos en conflicto.
- **RN-AG-04** (RN-04): entre dos turnos consecutivos que comparten **sillón o profesional** debe haber al menos **10 minutos** libres, para limpieza y preparación del sillón y para que el profesional cambie de sillón.
- **RN-AG-05** (RN-05): **casos de borde del margen.** Con un turno existente de 10:00 a 10:30 en el Sillón 1 con la Dra. A:
  - un turno nuevo a las **10:30** que comparte sillón o profesional → **se rechaza** (`MARGEN_INSUFICIENTE`);
  - un turno nuevo a las **10:40** que comparte sillón o profesional → **se acepta**;
  - un turno nuevo a las **10:30** con **otro profesional y otro sillón** → **se acepta** (no comparte recursos).
- **RN-AG-06** (RN-06): solo el **personal** (recepcionista, odontólogo o administrador) puede cargar un **sobreturno**. El sobreturno es una excepción explícita a RN-AG-02, RN-AG-03 y RN-AG-04, queda marcado (`es_sobreturno = true`) y se audita. *Si exige un motivo obligatorio está pendiente (PA-05).*
- **RN-AG-07** (RN-07): el **paciente** puede cancelar o reprogramar online **hasta 24 h antes** del inicio del turno. Después de ese plazo, solo puede hacerlo el personal. El error es `CANCELACION_FUERA_DE_PLAZO`.
- **RN-AG-08** (RN-08): al **cancelar** un turno, pasa a `CANCELADO` y el horario queda **libre** de inmediato para otra reserva. Reprogramar equivale a crear un turno nuevo válido y cancelar el anterior, en una única transacción.
- **RN-AG-09**: no se puede crear ni reprogramar un turno **en el pasado**.
- **RN-AG-10**: no se puede crear un turno que se superponga con un **bloqueo de agenda** del profesional.
- **RN-AG-11**: no se puede asignar un turno a un profesional, sillón o prestación **inactivos**.
- **RN-AG-12**: la verificación de conflicto y la creación del turno son **atómicas**. Si dos reservas compiten por el mismo hueco, solo una se confirma (DD-03).

## Dominio: Reserva online (RN-RO)

- **RN-RO-01**: el paciente solo ve **horarios libres** para una prestación. Nunca ve nombres ni datos de otros pacientes, ni estadísticas del consultorio.
- **RN-RO-02**: la disponibilidad que ve el paciente aplica las mismas reglas que la reserva del personal: RN-AG-01 a RN-AG-05, RN-AG-09 y RN-AG-10.
- **RN-RO-03**: el paciente **no puede** generar sobreturnos.

## Dominio: Notificaciones (RN-NO)

- **RN-NO-01** (RN-09): los recordatorios **no incluyen datos de salud**. Solo llevan el nombre del paciente, la fecha, la hora, el profesional y la dirección del consultorio, más los links de confirmar, cancelar o reprogramar.
- **RN-NO-02** (RN-09): el canal es **WhatsApp** si `tiene_whatsapp = true`; si no, **email**.
- **RN-NO-03**: solo se envían comunicaciones a pacientes con `consentimiento_comunicaciones = true` (Ley 25.326; ver DD-07).
- **RN-NO-04**: la anticipación del recordatorio es **a definir** (PA-08). Valor propuesto: 24 h antes.

## Dominio: Clínica (RN-CL)

- **RN-CL-01**: la ficha clínica y el odontograma solo los ve y los edita el rol `ODONTOLOGO` (y `ADMIN` en modo lectura). La recepción no accede (Ley 26.529, datos sensibles).
- **RN-CL-02**: los cambios en el odontograma se registran como **historial** (quién, cuándo, qué pieza y cara). No se sobrescribe un registro anterior.
- **RN-CL-03**: el odontograma usa la **notación FDI** e incluye dientes temporales (estándar de mercado según el informe, C.1).

## Dominio: Excepciones globales

- **RN-GL-01**: **solo datos ficticios** en el repositorio, los seeds, los tests, los ejemplos y las capturas.
- **RN-GL-02**: todas las fechas se guardan como `timestamptz` y se interpretan en la zona `America/Argentina/Buenos_Aires` (supuesto SU-03).
- **RN-GL-03**: toda creación, cancelación, reprogramación, confirmación o sobreturno queda en `TurnoAuditoria`.
