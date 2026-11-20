# Verificación: VecDent

- **Responsable**: Sofia Indovino
- **URL**: https://vectora.ar/pages/vecdent/
- **Tipo de fuente**: demo
- **Fecha y hora de consulta**: 2026-11-20 19:29

## Qué afirma el informe

Periodontograma de 6 puntos; portal del paciente; ARS 30.000, 50.000 y 80.000

## Qué encontré

- Afirmación 1 (periodontograma de 6 puntos): coincide. Detalle: por diente hay 3 puntos vestibulares (MV, V, DV) y 3 palatinos/linguales (ML, L, DL). Además registra recesión, sangrado y placa, movilidad y furcación, y calcula el estado del diente.
- Afirmación 2 (portal del paciente): no coincide. Detalle: por defecto, solo administrador, odontólogo y recepcionista crean turnos. Creé un rol personalizado con permisos limitados: no pudo crear turnos, y además veía el dashboard con estadísticas de la clínica y el menú Configuración. No hay un portal de autogestión del paciente.
- Afirmación 3 (precios ARS 30.000 / 50.000 / 80.000): coincide. Detalle: Básico $30.000, Profesional $50.000 y Premium $80.000 por mes. El sitio muestra "$" sin aclarar la moneda.

## Corrección propuesta

En A.2, cambiar VecDent / Reserva online del paciente a "No evidenciado (solo con rol interno personalizado)".

## Evidencia

- docs/discovery/verificacion/img/sofia-1.png
- docs/discovery/verificacion/img/sofia-3.png
