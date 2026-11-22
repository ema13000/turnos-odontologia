-- C-11 notificaciones-recordatorios. No edita V1, V3 ni V4: todo lo nuevo va acá (regla 3).

-- 1. Datos de comunicación del paciente (RN-NO-02, RN-NO-03). Todos arrancan en FALSE/NULL:
--    nadie recibe mensajes hasta que dé su consentimiento (la gestión del consentimiento es C-17).
ALTER TABLE paciente ADD COLUMN tiene_whatsapp BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE paciente ADD COLUMN consentimiento_comunicaciones BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE paciente ADD COLUMN consentimiento_fecha TIMESTAMPTZ;

-- 2. Notificaciones. 'REASIGNACION' (RN-AG-16) es un tipo nuevo respecto de la KB.
--    token_confirmacion guarda la huella SHA-256 (hexadecimal) del token, nunca el token.
CREATE TABLE notificacion (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    turno_id           BIGINT      NOT NULL REFERENCES turno (id),
    canal              VARCHAR(20) NOT NULL,
    tipo               VARCHAR(20) NOT NULL,
    estado             VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    programada_para    TIMESTAMPTZ NOT NULL,
    enviada_en         TIMESTAMPTZ,
    token_confirmacion VARCHAR(64),
    usado_en           TIMESTAMPTZ,
    intentos           INTEGER     NOT NULL DEFAULT 0,
    proximo_intento    TIMESTAMPTZ,
    canal_alternativo  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT notificacion_canal_valido CHECK (canal IN ('WHATSAPP', 'EMAIL')),
    CONSTRAINT notificacion_tipo_valido CHECK (tipo IN ('RECORDATORIO', 'CONFIRMACION', 'CANCELACION', 'REASIGNACION')),
    CONSTRAINT notificacion_estado_valido CHECK (estado IN ('PENDIENTE', 'ENVIADA', 'FALLIDA')),
    CONSTRAINT notificacion_intentos_validos CHECK (intentos >= 0),
    CONSTRAINT notificacion_token_unico UNIQUE (token_confirmacion)
);

-- Un solo recordatorio por turno, aunque dos corridas de la tarea elijan el mismo turno (design D5).
-- Es la garantía de la base; el filtro de la consulta es la validación previa.
CREATE UNIQUE INDEX notificacion_recordatorio_unico ON notificacion (turno_id) WHERE tipo = 'RECORDATORIO';
CREATE INDEX notificacion_turno_idx ON notificacion (turno_id);
CREATE INDEX notificacion_reintento_idx ON notificacion (proximo_intento) WHERE estado = 'FALLIDA';

-- 3. La auditoría suma CONFIRMAR y AUSENTE (esta última la usa C-13 para la ausencia del paciente). C-08 (V6)
--    reescribe esta misma restricción: por eso se hace idempotente y con las siete acciones.
ALTER TABLE turno_auditoria DROP CONSTRAINT IF EXISTS turno_auditoria_accion_valida;
ALTER TABLE turno_auditoria ADD CONSTRAINT turno_auditoria_accion_valida
    CHECK (accion IN ('CREAR', 'CANCELAR', 'REPROGRAMAR', 'SOBRETURNO', 'EXTENDER', 'CONFIRMAR', 'AUSENTE'));
