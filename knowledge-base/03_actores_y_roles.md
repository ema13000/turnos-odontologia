# Actores y Roles

## Actores del sistema

| Actor | Descripción | Cómo interactúa |
|---|---|---|
| **Recepcionista o secretaria** (`RECEPCIONISTA`) | Personal administrativo del consultorio | Panel web: carga, mueve y cancela turnos; carga sobreturnos; administra los datos de contacto de los pacientes |
| **Odontólogo** (`ODONTOLOGO`) | Profesional que atiende | Panel web: ve su agenda, bloquea horarios, carga sobreturnos, registra la ficha y el odontograma |
| **Administrador del consultorio** (`ADMIN`) | Dueño o encargado (puede ser el mismo odontólogo) | Panel web: configura profesionales, sillones, prestaciones y usuarios |
| **Paciente** (sin cuenta de personal) | Persona que se atiende | Página pública de reserva; links de confirmación, cancelación y reprogramación; recibe recordatorios |
| **Sistema de notificaciones** (actor técnico) | Tarea programada | Envía recordatorios por WhatsApp o email y procesa las confirmaciones |

**Segmento** (`discovery.md`, punto 2): odontólogo independiente (1 profesional y 1 sillón, a veces con secretaria) y consultorio chico (varios profesionales que comparten sillones).

## RBAC: matriz de permisos

C = crear, R = leer, U = modificar, D = eliminar o desactivar. "Propio" = solo sus propios turnos o datos.

| Recurso | ADMIN | RECEPCIONISTA | ODONTOLOGO | Paciente (público) |
|---|---|---|---|---|
| Profesionales, sillones, prestaciones | CRUD | R | R | R (solo nombre y prestaciones disponibles) |
| Usuarios del personal | CRUD | — | — | — |
| Turno (crear normal) | C | C | C | C (online, propio; RN-AG-07) |
| Turno (sobreturno) | C | C | C | **—** (RN-AG-06) |
| Turno (cancelar o reprogramar) | U | U (siempre) | U (siempre) | U propio, **hasta 24 h antes** (RN-AG-07) |
| Agenda (ver) | R | R | R (puede filtrar la propia) | **Solo disponibilidad**: horarios libres, sin datos de otros pacientes |
| Bloqueo de agenda | CRUD | — | CRUD propio | — |
| Horarios de trabajo | CRUD | R | R (propios) | R indirecta (solo ve los huecos libres resultantes) |
| Paciente (datos de contacto) | CRUD | CRUD | R | R/U propio (al reservar) |
| Ficha clínica y odontograma | R | **—** | CRUD | — |
| Estadísticas o dashboard | R | R (operativas) | R (propias) | **—** |

> **Lección de la verificación de fuentes:** en la demo de VecDent, un rol "paciente" armado a mano veía el dashboard con estadísticas de la clínica y el menú de configuración (informe, verificación de fuentes, fila 5). El paciente **no es un rol del personal con menos permisos**: solo accede a endpoints públicos y a sus propios turnos.

## Rutas públicas

Accesibles sin sesión del personal:

- `GET /api/v1/public/disponibilidad`: horarios libres por prestación y fecha. No expone nombres de pacientes.
- `POST /api/v1/public/reservas`: reserva online.
- `POST /api/v1/public/reservas/{codigo}/cancelar` y `/reprogramar`: con código o token de reserva (mecanismo a definir, ver PA-03).
- `POST /api/v1/public/confirmaciones/{token}`: confirmación desde el link del recordatorio.
- `POST /api/v1/webhooks/whatsapp`: webhook del proveedor. Valida la firma.
- Frontend: página de reserva del paciente.
