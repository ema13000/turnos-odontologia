# Modelo de Datos

## Dominios

| Dominio | Descripción |
|---|---|
| **Configuración** | Profesionales, sillones o boxes, prestaciones con duración |
| **Agenda** | Turnos, bloqueos, sobreturnos y la verificación de solapamiento con margen |
| **Pacientes** | Datos de contacto y preferencia de canal |
| **Notificaciones** | Recordatorios y confirmaciones por WhatsApp o email |
| **Clínica** | Ficha básica y odontograma |
| **Seguridad y auditoría** | Usuarios del personal, roles, registro de cambios en turnos |

Un despliegue corresponde a **un consultorio** (single-tenant en v1, ver DD-05).

## ERD

```
 Usuario (personal) ─0..1──1─ Profesional
                                  │1
                                  │
 Prestacion ─1──*─ Turno ─*──1─ Profesional
                    │ *
                    ├──1─ Sillon
                    ├──1─ Paciente ─1──0..1─ FichaClinica ─1──*─ OdontogramaRegistro
                    ├──*─ Notificacion
                    └──*─ TurnoAuditoria

 Profesional ─1──*─ BloqueoAgenda
```

## Entidades

### Profesional
- `id` UUID (PK), `nombre` varchar(120), `matricula` varchar(30) (único, ficticio en seed), `activo` boolean.
- Relaciones: 1:N `Turno`, 1:N `BloqueoAgenda`; 0..1 `Usuario`.

### Sillon
- `id` UUID (PK), `nombre` varchar(60) (único, por ejemplo "Sillón 1"), `activo` boolean.
- Relaciones: 1:N `Turno`.

### Prestacion
- `id` UUID (PK), `nombre` varchar(120) (único), `duracion_minutos` int, `activa` boolean.
- Constraints: `duracion_minutos > 0` y múltiplo de 5 (supuesto SU-06).
- RN-AG-01: la duración del turno se toma de acá.

### Paciente
- `id` UUID (PK), `dni` varchar(15) (único), `nombre` varchar(120), `telefono` varchar(30) (nullable), `email` varchar(160) (nullable), `tiene_whatsapp` boolean, `consentimiento_comunicaciones` boolean, `consentimiento_fecha` timestamptz.
- Constraints: `telefono` o `email` presente (al menos un canal de contacto).

### Turno
- `id` UUID (PK)
- `paciente_id` FK, `profesional_id` FK, `sillon_id` FK, `prestacion_id` FK
- `inicio` timestamptz, `fin` timestamptz (= inicio + duración de la prestación; RN-AG-01)
- `estado` enum: `RESERVADO`, `CONFIRMADO`, `CANCELADO`, `ATENDIDO`, `AUSENTE`
- `es_sobreturno` boolean, `motivo_sobreturno` varchar(255) (obligatoriedad a definir, ver PA-05)
- `origen` enum: `STAFF`, `ONLINE`
- `codigo_reserva` varchar(12) (único; para cancelar o reprogramar online)
- `creado_por` (usuario o "paciente-online"), `creado_en`, `cancelado_en`, `motivo_cancelacion`
- `version` int (bloqueo optimista)
- **Constraints**:
  - `fin > inicio`.
  - **Exclusión por sillón** (DD-03): no pueden coexistir dos turnos *no cancelados ni sobreturno* del mismo `sillon_id` cuyos rangos `[inicio, fin + 10 min)` se intersequen.
  - **Exclusión por profesional**: la misma restricción con `profesional_id`.
- Índices: `(profesional_id, inicio)`, `(sillon_id, inicio)`, `(paciente_id)`, `(estado, inicio)` para los recordatorios.

### BloqueoAgenda
- `id` UUID, `profesional_id` FK, `inicio`, `fin`, `motivo`.
- Un turno nuevo no puede caer dentro de un bloqueo del profesional (RN-AG-10).

### Notificacion
- `id` UUID, `turno_id` FK, `canal` enum (`WHATSAPP`, `EMAIL`), `tipo` enum (`RECORDATORIO`, `CONFIRMACION`, `CANCELACION`), `estado` enum (`PENDIENTE`, `ENVIADA`, `FALLIDA`), `programada_para`, `enviada_en`, `token_confirmacion` (único).
- RN-NO-01: el contenido no incluye datos de salud.

### FichaClinica
- `id` UUID, `paciente_id` FK (único), `antecedentes` text, `alergias` text, `medicacion` text, `actualizado_por`, `actualizado_en`.

### OdontogramaRegistro
- `id` UUID, `ficha_id` FK, `pieza` smallint (notación FDI: 11–48 permanentes, 51–85 temporales), `cara` enum (`O`, `M`, `D`, `V`, `L`/`P`), `estado` varchar(40) (por ejemplo `CARIES`, `OBTURADO`, `AUSENTE`, `A_TRATAR`), `profesional_id`, `registrado_en`.
- Historial: se agrega un registro nuevo en lugar de sobrescribir el anterior (trazabilidad).

### Usuario
- `id` UUID, `username` (único), `password_hash`, `rol` enum (`ADMIN`, `RECEPCIONISTA`, `ODONTOLOGO`), `profesional_id` FK nullable, `activo`.

### TurnoAuditoria
- `id`, `turno_id`, `accion` (`CREAR`, `CANCELAR`, `REPROGRAMAR`, `SOBRETURNO`, `CONFIRMAR`), `actor`, `fecha`, `detalle` jsonb.

## Seed data inicial

**Solo datos ficticios** (regla del proyecto; nunca datos reales de pacientes):

| Tipo | Datos |
|---|---|
| Profesionales | "Dra. Ana Ejemplo" (MP-0001), "Dr. Bruno Prueba" (MP-0002) |
| Sillones | "Sillón 1", "Sillón 2" |
| Prestaciones | Consulta o control (30 min), Limpieza (profilaxis) (45 min), Tratamiento de conducto (60 min), Extracción simple (30 min) |
| Usuarios | `admin`, `recepcion`, `ana.ejemplo` (contraseñas solo en el seed de desarrollo, nunca en producción) |
| Pacientes | 3 pacientes ficticios con DNI de la forma `99000001` y emails `@example.com` |
