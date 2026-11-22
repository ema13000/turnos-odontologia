package ar.turnos.odontologia.notificaciones.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.turnos.odontologia.notificaciones.IntegracionNotificacionesBase;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** Migración V7 (C-11): tabla notificacion, columnas de contacto del paciente y acción CONFIRMAR de la auditoría. */
@Tag("integration")
class V7MigrationIntegrationTest extends IntegracionNotificacionesBase {

    private long turnoId() {
        return turno(pacienteUno, draA, sillon1, turnoDeManana(), "RESERVADO");
    }

    private void notificacion(long turnoId, String canal, String tipo, String estado, String token) {
        jdbc.update("INSERT INTO notificacion(turno_id, canal, tipo, estado, programada_para, token_confirmacion)"
                + " VALUES (?,?,?,?,?,?)", turnoId, canal, tipo, estado, Timestamp.from(turnoDeManana()), token);
    }

    @Test
    @DisplayName("La tabla notificacion valida canal, tipo y estado")
    void validaCanalTipoYEstado() {
        long turno = turnoId();
        notificacion(turno, "WHATSAPP", "RECORDATORIO", "PENDIENTE", null);

        assertThatThrownBy(() -> notificacion(turno, "SMS", "REASIGNACION", "PENDIENTE", null))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("notificacion_canal_valido");
        assertThatThrownBy(() -> notificacion(turno, "EMAIL", "OTRO", "PENDIENTE", null))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("notificacion_tipo_valido");
        assertThatThrownBy(() -> notificacion(turno, "EMAIL", "REASIGNACION", "RARO", null))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("notificacion_estado_valido");
        // los tipos de la KB y el nuevo REASIGNACION se aceptan
        for (String tipo : List.of("CONFIRMACION", "CANCELACION", "REASIGNACION")) {
            notificacion(turno, "EMAIL", tipo, "ENVIADA", null);
        }
    }

    @Test
    @DisplayName("Un segundo recordatorio del mismo turno se rechaza y una reasignación repetida se acepta")
    void unSoloRecordatorioPorTurno() {
        long turno = turnoId();
        notificacion(turno, "WHATSAPP", "RECORDATORIO", "ENVIADA", null);

        assertThatThrownBy(() -> notificacion(turno, "EMAIL", "RECORDATORIO", "PENDIENTE", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("notificacion_recordatorio_unico");

        notificacion(turno, "EMAIL", "REASIGNACION", "ENVIADA", null);
        notificacion(turno, "EMAIL", "REASIGNACION", "ENVIADA", null);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacion WHERE tipo = 'REASIGNACION'", Integer.class))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("El token de confirmación es único")
    void tokenUnico() {
        long turno = turnoId();
        long otro = turno(pacienteDos, drB, sillon2, turnoDeManana(), "RESERVADO");
        String huella = "a".repeat(64);
        notificacion(turno, "WHATSAPP", "RECORDATORIO", "ENVIADA", huella);

        assertThatThrownBy(() -> notificacion(otro, "WHATSAPP", "RECORDATORIO", "ENVIADA", huella))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("notificacion_token_unico");
    }

    @Test
    @DisplayName("Un paciente existente queda sin consentimiento ni WhatsApp")
    void pacienteSinConsentimientoPorDefecto() {
        // pacienteUno se creó sin nombrar las columnas nuevas
        assertThat(jdbc.queryForMap("SELECT tiene_whatsapp, consentimiento_comunicaciones, consentimiento_fecha"
                + " FROM paciente WHERE id = ?", pacienteUno))
                .containsEntry("tiene_whatsapp", false)
                .containsEntry("consentimiento_comunicaciones", false)
                .containsEntry("consentimiento_fecha", null);
    }

    @Test
    @DisplayName("La auditoría acepta las seis acciones y rechaza otras")
    void auditoriaConSeisAcciones() {
        long turno = turnoId();
        for (String accion : List.of("CREAR", "CANCELAR", "REPROGRAMAR", "SOBRETURNO", "EXTENDER", "CONFIRMAR")) {
            jdbc.update("INSERT INTO turno_auditoria(turno_id, accion, actor, fecha) VALUES (?,?,'recepcion',now())",
                    turno, accion);
        }
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO turno_auditoria(turno_id, accion, actor, fecha) VALUES (?, 'BORRAR','recepcion',now())", turno))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("turno_auditoria_accion_valida");
    }

    @Test
    @DisplayName("Los dos EXCLUDE de turno siguen presentes")
    void siguenLosExclude() {
        assertThat(jdbc.queryForList("SELECT conname FROM pg_constraint WHERE conrelid = 'turno'::regclass"
                + " AND contype = 'x'", String.class))
                .containsExactlyInAnyOrder("turno_sin_solapamiento_sillon", "turno_sin_solapamiento_profesional");
    }
}
