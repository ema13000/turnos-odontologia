-- C-08 sobreturno y extensión. Migración nueva: no se editan V1, V3 ni V4 (regla dura 3).
-- No se tocan btree_gist ni las dos restricciones EXCLUDE (DD-03, regla dura 5).

-- 1. Extensión de un turno: el motivo es obligatorio cuando se marca extendido.
ALTER TABLE turno ADD COLUMN extendido BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE turno ADD COLUMN motivo_extension TEXT;
ALTER TABLE turno ADD CONSTRAINT turno_extension_motivo_valido CHECK (
    extendido = FALSE
        OR (motivo_extension IS NOT NULL AND length(btrim(motivo_extension)) BETWEEN 1 AND 500)
);

-- 2. Alinea la deriva documental de motivo_sobreturno (varchar(255) en la KB, VARCHAR(500) en V1)
--    usando TEXT + CHECK en una migración nueva.
ALTER TABLE turno DROP CONSTRAINT turno_sobreturno_motivo;
ALTER TABLE turno ALTER COLUMN motivo_sobreturno TYPE TEXT;
ALTER TABLE turno ADD CONSTRAINT turno_sobreturno_motivo CHECK (
    es_sobreturno = FALSE
        OR (motivo_sobreturno IS NOT NULL AND length(btrim(motivo_sobreturno)) BETWEEN 1 AND 500)
);

-- 3. RN-GL-03: las operaciones nuevas también se auditan.
ALTER TABLE turno_auditoria DROP CONSTRAINT turno_auditoria_accion_valida;
ALTER TABLE turno_auditoria ADD CONSTRAINT turno_auditoria_accion_valida
    CHECK (accion IN ('CREAR', 'CANCELAR', 'REPROGRAMAR', 'SOBRETURNO', 'EXTENDER'));
