-- C-06 api-turnos. Corrige la tabla turno de V1 sin editarla (regla 3) y agrega lo que necesita la API.

-- 1. Estado RESERVADO (lo usa el dominio de C-01) y como valor por defecto
ALTER TABLE turno DROP CONSTRAINT turno_estado_valido;
ALTER TABLE turno ADD CONSTRAINT turno_estado_valido
    CHECK (estado IN ('RESERVADO', 'CONFIRMADO', 'CANCELADO', 'AUSENTE', 'ATENDIDO'));
ALTER TABLE turno ALTER COLUMN estado SET DEFAULT 'RESERVADO';

-- 2. Solo los turnos activos (RESERVADO, CONFIRMADO) ocupan la agenda: un AUSENTE libera el horario (RN-AG-17).
--    Se conservan los nombres de las restricciones: la aplicación los usa para traducir el error.
ALTER TABLE turno DROP CONSTRAINT turno_sin_solapamiento_sillon;
ALTER TABLE turno ADD CONSTRAINT turno_sin_solapamiento_sillon EXCLUDE USING gist (
    sillon_id WITH =,
    tstzrange(inicio, fin_con_margen, '[)') WITH &&
) WHERE (estado IN ('RESERVADO', 'CONFIRMADO'));

ALTER TABLE turno DROP CONSTRAINT turno_sin_solapamiento_profesional;
ALTER TABLE turno ADD CONSTRAINT turno_sin_solapamiento_profesional EXCLUDE USING gist (
    profesional_id WITH =,
    tstzrange(inicio, fin_con_margen, '[)') WITH &&
) WHERE (estado IN ('RESERVADO', 'CONFIRMADO') AND es_sobreturno = FALSE);

-- 3. Número de reserva: XXXX-XXXX con un alfabeto sin caracteres ambiguos (sin 0 O 1 I L).
--    La aplicación genera el suyo con SecureRandom. La función solo cubre los INSERT directos
--    (filas de desarrollo y tests de la base) y usa el mismo alfabeto, así toda fila cumple el formato.
CREATE FUNCTION generar_codigo_reserva() RETURNS VARCHAR LANGUAGE sql VOLATILE AS $$
    SELECT substr(c, 1, 4) || '-' || substr(c, 5, 4)
    FROM (SELECT string_agg(substr('ABCDEFGHJKMNPQRSTUVWXYZ23456789', 1 + floor(random() * 31)::int, 1), '') AS c
          FROM generate_series(1, 8)) AS simbolos
$$;

ALTER TABLE turno ADD COLUMN codigo_reserva VARCHAR(12);
UPDATE turno SET codigo_reserva = generar_codigo_reserva();
ALTER TABLE turno ALTER COLUMN codigo_reserva SET DEFAULT generar_codigo_reserva();
ALTER TABLE turno ALTER COLUMN codigo_reserva SET NOT NULL;
ALTER TABLE turno ADD CONSTRAINT turno_codigo_reserva_key UNIQUE (codigo_reserva);
ALTER TABLE turno ADD CONSTRAINT turno_codigo_reserva_formato
    CHECK (codigo_reserva ~ '^[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{4}-[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{4}$');

-- 4. Auditoría de las operaciones sobre turnos (RN-GL-03). Sin texto libre: no caben datos sensibles.
CREATE TABLE turno_auditoria (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    turno_id             BIGINT       NOT NULL REFERENCES turno (id),
    accion               VARCHAR(20)  NOT NULL,
    actor                VARCHAR(120) NOT NULL,
    fecha                TIMESTAMPTZ  NOT NULL,
    turno_relacionado_id BIGINT       REFERENCES turno (id),

    CONSTRAINT turno_auditoria_accion_valida CHECK (accion IN ('CREAR', 'CANCELAR', 'REPROGRAMAR'))
);
CREATE INDEX turno_auditoria_turno_idx ON turno_auditoria (turno_id);
