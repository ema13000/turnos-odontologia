CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE profesional (
                             id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             nombre    VARCHAR(100) NOT NULL,
                             apellido  VARCHAR(100) NOT NULL,
                             matricula VARCHAR(30)  NOT NULL UNIQUE,
                             activo    BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE sillon (
                        id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        nombre VARCHAR(50) NOT NULL UNIQUE,
                        activo BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE prestacion (
                            id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            nombre           VARCHAR(100) NOT NULL UNIQUE,
                            duracion_minutos INTEGER      NOT NULL CHECK (duracion_minutos > 0),
                            activo           BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE paciente (
                          id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          nombre   VARCHAR(100) NOT NULL,
                          apellido VARCHAR(100) NOT NULL,
                          dni      VARCHAR(15)  NOT NULL UNIQUE,
                          email    VARCHAR(150),
                          telefono VARCHAR(30)
);

CREATE TABLE turno (
                       id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       profesional_id    BIGINT       NOT NULL REFERENCES profesional (id),
                       sillon_id         BIGINT       NOT NULL REFERENCES sillon (id),
                       prestacion_id     BIGINT       NOT NULL REFERENCES prestacion (id),
                       paciente_id       BIGINT       NOT NULL REFERENCES paciente (id),
                       inicio            TIMESTAMPTZ  NOT NULL,
                       fin               TIMESTAMPTZ  NOT NULL,
                       fin_con_margen    TIMESTAMPTZ  NOT NULL,
                       estado            VARCHAR(20)  NOT NULL DEFAULT 'CONFIRMADO',
                       es_sobreturno     BOOLEAN      NOT NULL DEFAULT FALSE,
                       motivo_sobreturno VARCHAR(500),
                       version           BIGINT       NOT NULL DEFAULT 0,

                       CONSTRAINT turno_estado_valido CHECK (estado IN ('CONFIRMADO', 'CANCELADO', 'AUSENTE', 'ATENDIDO')),
                       CONSTRAINT turno_fin_posterior CHECK (fin > inicio),
                       CONSTRAINT turno_margen_valido CHECK (fin_con_margen >= fin),
                       CONSTRAINT turno_sobreturno_motivo CHECK (
                           es_sobreturno = FALSE
                               OR (motivo_sobreturno IS NOT NULL AND length(btrim(motivo_sobreturno)) > 0)
                           ),

    -- Por sillón: incluye sobreturnos (nunca se pisa la limpieza del sillón)
                       CONSTRAINT turno_sin_solapamiento_sillon EXCLUDE USING gist (
        sillon_id WITH =,
        tstzrange(inicio, fin_con_margen, '[)') WITH &&
    ) WHERE (estado <> 'CANCELADO'),

    -- Por profesional: excluye sobreturnos (pueden pisar su agenda)
                       CONSTRAINT turno_sin_solapamiento_profesional EXCLUDE USING gist (
        profesional_id WITH =,
        tstzrange(inicio, fin_con_margen, '[)') WITH &&
    ) WHERE (estado <> 'CANCELADO' AND es_sobreturno = FALSE)
);