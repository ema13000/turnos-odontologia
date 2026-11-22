-- C-04 configuracion-consultorio: especialidades, sillón habitual y margen entre turnos.

-- Especialidades: qué prestaciones hace cada profesional (RN-AG-15).
CREATE TABLE profesional_prestacion (
    profesional_id BIGINT NOT NULL REFERENCES profesional (id),
    prestacion_id  BIGINT NOT NULL REFERENCES prestacion (id),
    PRIMARY KEY (profesional_id, prestacion_id)
);

-- Sillón habitual del profesional (US-018, RN-RO-06). Opcional.
ALTER TABLE profesional
    ADD COLUMN sillon_habitual_id BIGINT REFERENCES sillon (id);

-- La duración de una prestación es múltiplo de 5 (SU-06). El dominio también lo valida.
ALTER TABLE prestacion
    ADD CONSTRAINT prestacion_duracion_multiplo_5 CHECK (duracion_minutos % 5 = 0);

-- Configuración del consultorio en pares clave y valor.
CREATE TABLE configuracion (
    clave VARCHAR(50)  PRIMARY KEY,
    valor VARCHAR(100) NOT NULL
);

-- Margen predeterminado entre turnos (RN-AG-04): 10 minutos.
INSERT INTO configuracion (clave, valor) VALUES ('margen_minutos', '10');
