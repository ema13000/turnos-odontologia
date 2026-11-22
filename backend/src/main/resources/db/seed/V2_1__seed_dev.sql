-- C-04: datos FICTICIOS para el entorno de desarrollo (perfil dev).
-- Fuente: knowledge-base/04_modelo_de_datos.md, sección "Seed data inicial".
-- Nunca usar datos reales de personas.

INSERT INTO sillon (nombre) VALUES ('Sillón 1'), ('Sillón 2');

INSERT INTO prestacion (nombre, duracion_minutos) VALUES
    ('Consulta o control', 30),
    ('Limpieza (profilaxis)', 45),
    ('Tratamiento de conducto', 60),
    ('Extracción simple', 30);

INSERT INTO profesional (nombre, apellido, matricula, sillon_habitual_id) VALUES
    ('Ana', 'Ejemplo', 'MP-0001', (SELECT id FROM sillon WHERE nombre = 'Sillón 1')),
    ('Bruno', 'Prueba', 'MP-0002', (SELECT id FROM sillon WHERE nombre = 'Sillón 2'));

-- Los dos hacen Consulta, Limpieza y Extracción.
INSERT INTO profesional_prestacion (profesional_id, prestacion_id)
SELECT p.id, pr.id
FROM profesional p
CROSS JOIN prestacion pr
WHERE p.matricula IN ('MP-0001', 'MP-0002')
  AND pr.nombre IN ('Consulta o control', 'Limpieza (profilaxis)', 'Extracción simple');

-- Solo el Dr. Bruno Prueba hace Tratamiento de conducto (para probar RN-AG-15).
INSERT INTO profesional_prestacion (profesional_id, prestacion_id)
SELECT p.id, pr.id
FROM profesional p, prestacion pr
WHERE p.matricula = 'MP-0002'
  AND pr.nombre = 'Tratamiento de conducto';
