# Funcionalidades

Están organizadas por **épica** y, dentro de cada una, por **historia de usuario** (US-NNN). Su prioridad sigue la sección D.3 del informe de Discovery.

## Épica 1: Configuración del consultorio (v1)

### US-001: Gestionar profesionales
**Como** administrador **quiero** dar de alta, modificar y desactivar profesionales **para** asignarles turnos.
**Criterios de aceptación**:
- [ ] CA-1: un profesional desactivado no aparece en la disponibilidad ni admite turnos nuevos.
- [ ] CA-2: la matrícula es única.
**Reglas relacionadas**: RN-AG-11

### US-002: Gestionar sillones o boxes
**Como** administrador **quiero** registrar los sillones del consultorio **para** que la agenda controle su ocupación.
**Criterios de aceptación**:
- [ ] CA-1: el nombre del sillón es único.
- [ ] CA-2: un sillón desactivado no admite turnos nuevos.
**Reglas relacionadas**: RN-AG-03, RN-AG-11

### US-003: Gestionar prestaciones con duración
**Como** administrador **quiero** definir prestaciones con su duración en minutos **para** que el fin del turno se calcule solo.
**Criterios de aceptación**:
- [ ] CA-1: la duración es mayor que 0 y múltiplo de 5.
- [ ] CA-2: cambiar la duración no modifica los turnos ya creados.
**Reglas relacionadas**: RN-AG-01

## Épica 2: Agenda y turnos (v1, núcleo)

### US-010: Crear un turno sin solapamientos ⭐ *(candidato al change OPSX)*
**Como** recepcionista **quiero** crear un turno eligiendo paciente, profesional, sillón, prestación e inicio **para** que el sistema me impida superponerlo con otro turno del mismo profesional o del mismo sillón.
**Criterios de aceptación**:
- [ ] CA-1 (feliz): si no hay conflicto, el turno se crea en estado `RESERVADO` con `fin = inicio + duración`.
- [ ] CA-2 (error de negocio): si el profesional tiene un turno activo en conflicto, se rechaza con `SOLAPAMIENTO_PROFESIONAL`.
- [ ] CA-3 (error de negocio): si el sillón tiene un turno activo en conflicto, se rechaza con `SOLAPAMIENTO_SILLON`.
- [ ] CA-4 (borde): con un turno existente de 10:00 a 10:30 que comparte sillón o profesional, un turno a las 10:30 se rechaza (`MARGEN_INSUFICIENTE`) y uno a las 10:40 se acepta.
- [ ] CA-5 (borde): un turno a las 10:30 con otro profesional y otro sillón se acepta.
- [ ] CA-6: los turnos `CANCELADO` no generan conflicto.
**Reglas relacionadas**: RN-AG-01 a RN-AG-05, RN-AG-09, RN-AG-11, RN-AG-12

### US-011: Cancelar un turno y liberar el horario
**Como** recepcionista **quiero** cancelar un turno **para** que el horario quede disponible.
**Criterios de aceptación**:
- [ ] CA-1: el turno pasa a `CANCELADO` y el hueco se puede reservar enseguida.
- [ ] CA-2: queda registrado en la auditoría con el motivo.
**Reglas relacionadas**: RN-AG-08, RN-GL-03

### US-012: Reprogramar un turno
**Como** recepcionista **quiero** mover un turno a otro horario **para** acomodar al paciente sin perder la reserva.
**Criterios de aceptación**:
- [ ] CA-1: el nuevo horario cumple las mismas reglas que un turno nuevo.
- [ ] CA-2: si el nuevo horario está en conflicto, el turno original queda intacto.
**Reglas relacionadas**: RN-AG-08, RN-AG-02 a RN-AG-05

### US-013: Cargar un sobreturno
**Como** odontólogo o recepcionista **quiero** cargar un sobreturno **para** atender una urgencia aunque el horario esté ocupado.
**Criterios de aceptación**:
- [ ] CA-1: solo el personal puede hacerlo; el paciente recibe `403`.
- [ ] CA-2: el turno queda marcado como sobreturno y auditado.
**Reglas relacionadas**: RN-AG-06, RN-RO-03

### US-014: Ver la agenda diaria y semanal
**Como** odontólogo o recepcionista **quiero** ver la agenda por día y semana, filtrada por profesional o por sillón, **para** saber qué está libre.
**Criterios de aceptación**:
- [ ] CA-1: se distinguen visualmente los sobreturnos y los turnos confirmados.
- [ ] CA-2: el diseño es responsivo y se usa desde el celular.

### US-015: Bloquear horarios
**Como** odontólogo **quiero** bloquear franjas de mi agenda **para** que no me asignen turnos.
**Reglas relacionadas**: RN-AG-10

## Épica 3: Reserva online del paciente (v1)

### US-020: Consultar disponibilidad
**Como** paciente **quiero** ver los horarios libres para una prestación **para** elegir cuándo atenderme.
**Criterios de aceptación**:
- [ ] CA-1: solo se muestran horarios que cumplen las reglas de la agenda.
- [ ] CA-2: no se exponen datos de otros pacientes.
**Reglas relacionadas**: RN-RO-01, RN-RO-02

### US-021: Reservar un turno online
**Como** paciente **quiero** reservar sin llamar **para** ahorrar tiempo.
**Criterios de aceptación**:
- [ ] CA-1: recibo un código de reserva y un recordatorio por el canal que corresponde.
- [ ] CA-2: si otra persona tomó el horario mientras tanto, recibo un error claro y otras opciones.
**Reglas relacionadas**: RN-AG-12, RN-NO-02

### US-022: Cancelar o reprogramar online
**Como** paciente **quiero** cancelar o reprogramar desde el link **para** liberar el horario.
**Criterios de aceptación**:
- [ ] CA-1: hasta 24 h antes, se permite.
- [ ] CA-2: con menos de 24 h, se rechaza con `CANCELACION_FUERA_DE_PLAZO` y se indica que hay que contactar al consultorio.
**Reglas relacionadas**: RN-AG-07, RN-AG-08

## Épica 4: Recordatorios y confirmación (v1)

### US-030: Recibir un recordatorio
**Como** paciente **quiero** un recordatorio por WhatsApp, o por email si no tengo WhatsApp, **para** no olvidarme del turno.
**Criterios de aceptación**:
- [ ] CA-1: el mensaje no contiene datos de salud.
- [ ] CA-2: solo se envía con consentimiento.
**Reglas relacionadas**: RN-NO-01 a RN-NO-04

### US-031: Confirmar asistencia
**Como** paciente **quiero** confirmar desde el recordatorio **para** que el consultorio sepa que voy.
**Criterios de aceptación**:
- [ ] CA-1: el turno pasa a `CONFIRMADO`.
- [ ] CA-2: el token es de un solo uso.

## Épica 5: Ficha clínica (v1)

### US-040: Registrar la ficha básica
**Como** odontólogo **quiero** registrar antecedentes, alergias y medicación **para** atender de forma segura.
**Reglas relacionadas**: RN-CL-01

### US-041: Registrar el odontograma
**Como** odontólogo **quiero** marcar el estado de cada pieza y cara con notación FDI **para** llevar el historial del paciente.
**Reglas relacionadas**: RN-CL-02, RN-CL-03

## Backlog: v1.x y posteriores (fuera del MVP)

| ID | Funcionalidad | Etapa | Origen |
|---|---|---|---|
| US-100 | Lista de espera automática al liberarse un horario | v1.x | Vacío del mercado: 0 de 16 sistemas (informe, C.3) |
| US-101 | Política de cancelación configurable por consultorio | v1.x | Informe, C.4 |
| US-102 | Registro de consentimiento para comunicaciones con una pantalla propia | v1.x | Informe, D.3 |
| US-200 | Obras sociales y prepagas (nomencladores, liquidaciones) | Posterior | Informe, D.3 |
| US-201 | Facturación electrónica ARCA | Posterior | Informe, D.3 |
| US-202 | Señas con Mercado Pago | Posterior | Informe, D.3 |
| US-203 | Periodontograma, imágenes y radiografías | Posterior | Informe, D.3 |
| US-204 | Asistente con IA, multisucursal, exportación HL7 FHIR | Posterior | Informe, D.3 |
