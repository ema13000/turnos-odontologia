CREATE TABLE usuario (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username       TEXT        NOT NULL,
    password_hash  TEXT        NOT NULL,
    rol            TEXT        NOT NULL,
    profesional_id BIGINT      REFERENCES profesional (id),
    paciente_id    BIGINT      REFERENCES paciente (id),
    activo         BOOLEAN     NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT usuario_username_unico UNIQUE (username),
    CONSTRAINT usuario_paciente_unico UNIQUE (paciente_id),
    CONSTRAINT usuario_profesional_unico UNIQUE (profesional_id),
    CONSTRAINT usuario_username_normalizado CHECK (
        username = lower(btrim(username)) AND length(username) BETWEEN 3 AND 150),
    CONSTRAINT usuario_rol_valido CHECK (rol IN ('ADMIN', 'RECEPCIONISTA', 'ODONTOLOGO', 'PACIENTE')),
    -- Un usuario PACIENTE tiene paciente_id y ningún otro rol lo tiene
    CONSTRAINT usuario_paciente_vinculo CHECK ((rol = 'PACIENTE') = (paciente_id IS NOT NULL)),
    -- profesional_id solo para ODONTOLOGO o ADMIN (el dueño puede ser el mismo odontólogo)
    CONSTRAINT usuario_profesional_vinculo CHECK (
        profesional_id IS NULL OR rol IN ('ODONTOLOGO', 'ADMIN')),
    CONSTRAINT usuario_odontologo_con_profesional CHECK (rol <> 'ODONTOLOGO' OR profesional_id IS NOT NULL)
);
