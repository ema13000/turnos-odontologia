package ar.turnos.odontologia.notificaciones.infrastructure;

import static ar.turnos.odontologia.notificaciones.infrastructure.TurnosARecordarJdbc.fecha;
import static ar.turnos.odontologia.notificaciones.infrastructure.TurnosARecordarJdbc.instante;

import ar.turnos.odontologia.notificaciones.application.NotificacionRepository;
import ar.turnos.odontologia.notificaciones.domain.Canal;
import ar.turnos.odontologia.notificaciones.domain.EstadoNotificacion;
import ar.turnos.odontologia.notificaciones.domain.Notificacion;
import ar.turnos.odontologia.notificaciones.domain.TipoNotificacion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Adaptador de {@link NotificacionRepository} con SQL nativo. Cada operación es una sola sentencia, así es atómica
 * por sí sola y, si hay una transacción abierta (la confirmación), participa de ella.
 *
 * <p>La unicidad del recordatorio la decide la base: {@code INSERT ... ON CONFLICT DO NOTHING} sobre el índice
 * único parcial. No aborta la transacción ante el conflicto (en PostgreSQL una violación de restricción sí lo
 * haría), y la cantidad de filas insertadas dice quién ganó. El consumo del token es un {@code UPDATE} condicional
 * que también se resuelve por filas afectadas.
 */
@Repository
class NotificacionRepositoryJdbc implements NotificacionRepository {

    private static final String COLUMNAS = """
            n.id, n.turno_id, n.canal, n.tipo, n.estado, n.programada_para, n.enviada_en, n.token_confirmacion,
            n.usado_en, n.intentos, n.proximo_intento, n.canal_alternativo
            """;

    private static final RowMapper<Notificacion> NOTIFICACION = (rs, fila) -> leer(rs);

    private final JdbcTemplate jdbc;

    NotificacionRepositoryJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean existeRecordatorio(Long turnoId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM notificacion WHERE turno_id = ? AND tipo = 'RECORDATORIO')",
                Boolean.class, turnoId));
    }

    @Override
    public Optional<Notificacion> insertarRecordatorio(Notificacion n) {
        List<Long> ids = jdbc.queryForList("""
                INSERT INTO notificacion(turno_id, canal, tipo, estado, programada_para, token_confirmacion,
                                         intentos, canal_alternativo)
                VALUES (?, ?, 'RECORDATORIO', ?, ?, ?, ?, ?)
                ON CONFLICT (turno_id) WHERE tipo = 'RECORDATORIO' DO NOTHING
                RETURNING id
                """, Long.class, n.turnoId(), n.canal().name(), n.estado().name(), fecha(n.programadaPara()),
                n.tokenConfirmacion(), n.intentos(), n.canalAlternativo());
        return ids.stream().findFirst().map(n::conId);
    }

    @Override
    public Notificacion insertar(Notificacion n) {
        Long id = jdbc.queryForObject("""
                INSERT INTO notificacion(turno_id, canal, tipo, estado, programada_para, token_confirmacion,
                                         intentos, canal_alternativo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, n.turnoId(), n.canal().name(), n.tipo().name(), n.estado().name(),
                fecha(n.programadaPara()), n.tokenConfirmacion(), n.intentos(), n.canalAlternativo());
        return n.conId(id);
    }

    @Override
    public void actualizar(Notificacion n) {
        jdbc.update("""
                UPDATE notificacion
                SET canal = ?, estado = ?, enviada_en = ?, token_confirmacion = ?, intentos = ?,
                    proximo_intento = ?, canal_alternativo = ?
                WHERE id = ?
                """, n.canal().name(), n.estado().name(), fecha(n.enviadaEn()), n.tokenConfirmacion(), n.intentos(),
                fecha(n.proximoIntento()), n.canalAlternativo(), n.id());
    }

    @Override
    public List<Reintento> reintentosVencidos(Instant ahora) {
        String sql = "SELECT " + COLUMNAS + ", " + TurnosARecordarJdbc.COLUMNAS + """
                FROM notificacion n
                JOIN turno t ON t.id = n.turno_id
                JOIN paciente pa ON pa.id = t.paciente_id
                WHERE (n.estado = 'FALLIDA' AND n.proximo_intento IS NOT NULL AND n.proximo_intento <= ?)
                   OR (n.estado = 'PENDIENTE' AND n.programada_para <= ?)
                ORDER BY n.programada_para, n.id
                """;
        return jdbc.query(sql,
                (rs, fila) -> new Reintento(leer(rs), TurnosARecordarJdbc.leer(rs, false)), fecha(ahora),
                fecha(ahora.minus(Notificacion.ESPERA_PENDIENTE)));
    }

    /**
     * UPDATE condicional: solo gana quien la encuentra todavía vencida en el momento de escribir. Con dos corridas
     * simultáneas, la segunda espera el bloqueo de la fila, vuelve a evaluar la condición (ya es PENDIENTE reciente)
     * y no afecta ninguna fila. No hay violación de restricción, así que no se aborta ninguna transacción.
     */
    @Override
    public boolean reclamar(Notificacion vista, Instant ahora) {
        return jdbc.update("""
                UPDATE notificacion
                SET estado = 'PENDIENTE', programada_para = ?, proximo_intento = NULL
                WHERE id = ?
                  AND ((estado = 'FALLIDA' AND proximo_intento IS NOT NULL AND proximo_intento <= ?)
                       OR (estado = 'PENDIENTE' AND programada_para <= ?))
                """, fecha(ahora), vista.id(), fecha(ahora), fecha(ahora.minus(Notificacion.ESPERA_PENDIENTE))) == 1;
    }

    @Override
    public Optional<Notificacion> buscarPorHuella(String huella) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM notificacion n WHERE n.token_confirmacion = ?",
                NOTIFICACION, huella).stream().findFirst();
    }

    @Override
    public boolean consumirToken(String huella, Instant ahora) {
        return jdbc.update("UPDATE notificacion SET usado_en = ? WHERE token_confirmacion = ? AND usado_en IS NULL",
                fecha(ahora), huella) == 1;
    }

    private static Notificacion leer(ResultSet rs) throws SQLException {
        return new Notificacion(rs.getLong("id"), rs.getLong("turno_id"), Canal.valueOf(rs.getString("canal")),
                TipoNotificacion.valueOf(rs.getString("tipo")), EstadoNotificacion.valueOf(rs.getString("estado")),
                instante(rs, "programada_para"), instante(rs, "enviada_en"), rs.getString("token_confirmacion"),
                instante(rs, "usado_en"), rs.getInt("intentos"), instante(rs, "proximo_intento"),
                rs.getBoolean("canal_alternativo"));
    }
}
