-- C-07: horarios de trabajo FICTICIOS para el entorno de desarrollo (perfil dev).
-- Fuente: knowledge-base/04_modelo_de_datos.md, sección "Seed data inicial".
-- Se superponen de 10 a 13 para probar la reserva "sin preferencia" (C-10).

-- Dra. Ana Ejemplo: lunes a viernes de 08:00 a 13:00.
INSERT INTO horario_trabajo (profesional_id, dia_semana, hora_desde, hora_hasta)
SELECT p.id, d.dia, TIME '08:00', TIME '13:00'
FROM profesional p
CROSS JOIN generate_series(1, 5) AS d(dia)
WHERE p.matricula = 'MP-0001';

-- Dr. Bruno Prueba: lunes a viernes de 10:00 a 18:00.
INSERT INTO horario_trabajo (profesional_id, dia_semana, hora_desde, hora_hasta)
SELECT p.id, d.dia, TIME '10:00', TIME '18:00'
FROM profesional p
CROSS JOIN generate_series(1, 5) AS d(dia)
WHERE p.matricula = 'MP-0002';
