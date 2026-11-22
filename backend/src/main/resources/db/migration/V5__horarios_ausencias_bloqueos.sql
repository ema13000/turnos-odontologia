-- C-07 horarios-ausencias-bloqueos: franjas de trabajo, ausencias y licencias, y bloqueos de agenda por profesional.
-- Las reglas (RN-AG-10, RN-AG-13, RN-AG-16) las aplica el dominio de la agenda; acá van las restricciones de datos.

-- Franjas semanales de trabajo (RN-AG-13, DD-08). dia_semana sigue ISO-8601: 1 = lunes … 7 = domingo.
-- Las horas son locales de America/Argentina/Buenos_Aires. Ninguna franja cruza la medianoche.
CREATE TABLE horario_trabajo (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    profesional_id BIGINT   NOT NULL REFERENCES profesional (id),
    dia_semana     SMALLINT NOT NULL,
    hora_desde     TIME     NOT NULL,
    hora_hasta     TIME     NOT NULL,

    CONSTRAINT horario_trabajo_dia_valido CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT horario_trabajo_rango_valido CHECK (hora_hasta > hora_desde),
    -- Dos franjas del mismo profesional y día no se superponen; dos contiguas (8-12 y 12-14) sí se aceptan.
    -- La fecha fija solo sirve para convertir las horas en un rango comparable con btree_gist.
    CONSTRAINT horario_trabajo_sin_superposicion EXCLUDE USING gist (
        profesional_id WITH =,
        dia_semana WITH =,
        tsrange(DATE '2000-01-03' + hora_desde, DATE '2000-01-03' + hora_hasta, '[)') WITH &&
    )
);

-- Ausencias (por ejemplo, enfermedad) y licencias (por ejemplo, vacaciones) del profesional (RN-AG-16).
-- El motivo es opcional y puede ser sensible: no se expone en turnos ni va a logs.
CREATE TABLE ausencia_profesional (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    profesional_id BIGINT       NOT NULL REFERENCES profesional (id),
    tipo           VARCHAR(10)  NOT NULL,
    inicio         TIMESTAMPTZ  NOT NULL,
    fin            TIMESTAMPTZ  NOT NULL,
    motivo         VARCHAR(255),
    registrado_por VARCHAR(120) NOT NULL,
    registrado_en  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT ausencia_profesional_tipo_valido CHECK (tipo IN ('AUSENCIA', 'LICENCIA')),
    CONSTRAINT ausencia_profesional_periodo_valido CHECK (fin > inicio)
);
CREATE INDEX ausencia_profesional_periodo_idx ON ausencia_profesional (profesional_id, inicio, fin);

-- Bloqueos de agenda del profesional (RN-AG-10, US-015).
CREATE TABLE bloqueo_agenda (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    profesional_id BIGINT       NOT NULL REFERENCES profesional (id),
    inicio         TIMESTAMPTZ  NOT NULL,
    fin            TIMESTAMPTZ  NOT NULL,
    motivo         VARCHAR(255),
    registrado_por VARCHAR(120) NOT NULL,
    registrado_en  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT bloqueo_agenda_periodo_valido CHECK (fin > inicio)
);
CREATE INDEX bloqueo_agenda_periodo_idx ON bloqueo_agenda (profesional_id, inicio, fin);
