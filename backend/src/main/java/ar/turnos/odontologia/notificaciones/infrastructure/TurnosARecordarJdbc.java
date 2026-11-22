package ar.turnos.odontologia.notificaciones.infrastructure;

import ar.turnos.odontologia.notificaciones.application.TurnosARecordar;
import ar.turnos.odontologia.notificaciones.domain.ContactoPaciente;
import ar.turnos.odontologia.notificaciones.domain.TurnoARecordar;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Consulta propia de {@code notificaciones} sobre turno, paciente, profesional y auditoría (design D2). Devuelve
 * solo lo necesario para el mensaje: nunca la prestación ni el motivo de un sobreturno, aunque estén en la tabla.
 *
 * <p>Se usa SQL nativo con {@link JdbcTemplate}, que comparte la transacción de JPA cuando hay una abierta: la
 * consulta necesita {@code LATERAL} y {@code INSERT ... ON CONFLICT}, que JPQL no expresa.
 */
@Repository
class TurnosARecordarJdbc implements TurnosARecordar {

    /** Las columnas que arman un {@code TurnoARecordar}; {@code reservado_en} es la primera auditoría de alta. */
    static final String COLUMNAS = """
            t.id AS turno_id, t.inicio, t.estado AS turno_estado,
            pa.email, pa.telefono, pa.tiene_whatsapp, pa.consentimiento_comunicaciones
            """;

    private static final String DESDE = """
            FROM turno t
            JOIN paciente pa ON pa.id = t.paciente_id
            LEFT JOIN LATERAL (
                SELECT min(a.fecha) AS reservado_en
                FROM turno_auditoria a
                WHERE a.turno_id = t.id AND a.accion IN ('CREAR', 'SOBRETURNO')
            ) reserva ON TRUE
            """;

    private final JdbcTemplate jdbc;

    TurnosARecordarJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<TurnoARecordar> candidatos(Instant ahora) {
        String sql = "SELECT " + COLUMNAS + ", reserva.reservado_en " + DESDE + """
                WHERE t.estado IN ('RESERVADO', 'CONFIRMADO')
                  AND t.inicio > ? AND t.inicio <= ?
                  AND pa.consentimiento_comunicaciones
                  AND NOT EXISTS (SELECT 1 FROM notificacion n WHERE n.turno_id = t.id AND n.tipo = 'RECORDATORIO')
                  AND (reserva.reservado_en IS NULL OR t.inicio - reserva.reservado_en >= interval '24 hours')
                ORDER BY t.inicio, t.id
                """;
        return jdbc.query(sql, (rs, fila) -> leer(rs, true),
                fecha(ahora), fecha(ahora.plus(TurnoARecordar.ANTICIPACION)));
    }

    @Override
    public Optional<TurnoARecordar> deTurno(Long turnoId) {
        String sql = "SELECT " + COLUMNAS + ", reserva.reservado_en " + DESDE + " WHERE t.id = ?";
        return jdbc.query(sql, (rs, fila) -> leer(rs, true), turnoId).stream().findFirst();
    }

    /** Lee el turno de una fila con {@link #COLUMNAS}; {@code conReserva} indica si está {@code reservado_en}. */
    static TurnoARecordar leer(ResultSet rs, boolean conReserva) throws SQLException {
        String estado = rs.getString("turno_estado");
        return new TurnoARecordar(
                rs.getLong("turno_id"),
                instante(rs, "inicio"),
                conReserva ? instante(rs, "reservado_en") : null,
                "RESERVADO".equals(estado) || "CONFIRMADO".equals(estado),
                new ContactoPaciente(rs.getString("email"), rs.getString("telefono"),
                        rs.getBoolean("tiene_whatsapp"), rs.getBoolean("consentimiento_comunicaciones")));
    }

    static Instant instante(ResultSet rs, String columna) throws SQLException {
        OffsetDateTime valor = rs.getObject(columna, OffsetDateTime.class);
        return valor == null ? null : valor.toInstant();
    }

    static OffsetDateTime fecha(Instant instante) {
        return instante == null ? null : instante.atOffset(ZoneOffset.UTC);
    }
}
