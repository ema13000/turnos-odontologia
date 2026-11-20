# Flujos Principales

## Flujo 1: Crear un turno (personal) ⭐ núcleo del MVP

**Disparador**: la recepcionista carga un turno en el panel.
**Actor**: recepcionista u odontólogo.

**Pasos**:
1. **Frontend** envía `POST /api/v1/turnos` con `pacienteId`, `profesionalId`, `sillonId`, `prestacionId` e `inicio`.
2. **API** valida la entrada (campos obligatorios, formato de fecha) y la sesión y el rol.
3. **Caso de uso `CrearTurno`** carga la prestación y calcula `fin = inicio + duración` (RN-AG-01).
4. Verifica que el inicio no sea pasado (RN-AG-09) y que los recursos estén activos (RN-AG-11).
5. Consulta los **turnos activos** del profesional y del sillón en la ventana `[inicio − duraciónMáx − MARGEN, fin + MARGEN]`.
6. **Dominio `Agenda`** evalúa el conflicto con margen (RN-AG-02 a RN-AG-05) y los bloqueos (RN-AG-10).
7. Si no hay conflicto, **persiste** el turno en una transacción. La **restricción de exclusión** de PostgreSQL repite la verificación (RN-AG-12).
8. Registra la **auditoría** y programa el **recordatorio**.
9. Responde `201 Created` con el turno.

```
Recepción → Frontend → API → CrearTurno → Agenda(dominio) → TurnoRepository → PostgreSQL
                                   │            ↑ conflicto?          │ EXCLUDE (garantía final)
                                   └────────────┴─ 409 si hay choque ←─┘
```

**Casos de error**:
- Choque con un turno del mismo profesional → `409 SOLAPAMIENTO_PROFESIONAL`.
- Choque con un turno del mismo sillón → `409 SOLAPAMIENTO_SILLON`.
- Contiguo sin los 10 minutos de margen → `409 MARGEN_INSUFICIENTE`.
- Dos reservas concurrentes en el mismo hueco: una gana y la otra falla por la restricción de exclusión, que se traduce a `409`.
- Inicio en el pasado o recurso inactivo → `422`.

## Flujo 2: Reserva online del paciente

**Disparador**: el paciente entra a la página pública de reserva.
**Actor**: paciente.

**Pasos**:
1. Elige una prestación y una fecha.
2. `GET /public/disponibilidad` calcula los huecos libres con las mismas reglas que el Flujo 1 (RN-RO-02) y devuelve solo los horarios.
3. El paciente elige un horario y completa sus datos (DNI, nombre, contacto, canal, consentimiento).
4. `POST /public/reservas` ejecuta el mismo caso de uso que el Flujo 1, con `origen = ONLINE` y sin opción de sobreturno.
5. Se devuelve el **código de reserva** y se envía la confirmación por el canal que corresponde.

**Casos de error**:
- El horario se ocupó entre la consulta y la reserva → `409` con alternativas.
- Datos de contacto faltantes → `422`.

## Flujo 3: Cancelar o reprogramar

**Disparador**: el paciente (desde el link o con el código) o el personal (desde el panel).

**Pasos**:
1. Se identifica el turno (por `id` para el personal, o por código o token para el paciente).
2. Si el actor es el paciente: se verifica que falten **24 h o más** para el inicio (RN-AG-07).
3. **Cancelar**: el estado pasa a `CANCELADO` y el hueco queda libre (RN-AG-08).
4. **Reprogramar**: en una transacción se crea el turno nuevo con las reglas del Flujo 1 y se cancela el original. Si el nuevo falla, el original no cambia.
5. Se registra la auditoría y se notifica al paciente.

**Casos de error**:
- Paciente con menos de 24 h de anticipación → `409 CANCELACION_FUERA_DE_PLAZO`.
- Nuevo horario en conflicto → `409` (el original queda intacto).

## Flujo 4: Recordatorio y confirmación

**Disparador**: una tarea programada (`@Scheduled`) cada N minutos.

**Pasos**:
1. Busca los turnos `RESERVADO` cuyo inicio cae en la ventana de recordatorio (PA-08; propuesto: 24 h).
2. Filtra los pacientes con consentimiento (RN-NO-03).
3. Elige el canal: WhatsApp si `tiene_whatsapp`; si no, email (RN-NO-02).
4. Arma el mensaje **sin datos de salud** (RN-NO-01), con el link de confirmar, cancelar o reprogramar.
5. Lo envía por `NotificacionPort`. En el TP, el adaptador es simulado.
6. Cuando el paciente confirma (link o webhook), el turno pasa a `CONFIRMADO`.

**Casos de error**:
- Falla del proveedor → `Notificacion.estado = FALLIDA` y reintento con backoff; después de N fallas, se envía por email si hay email.
