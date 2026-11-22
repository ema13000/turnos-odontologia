package ar.turnos.odontologia.shared.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Esquema de V6: columnas nuevas, CHECKs y acciones de auditoría (PostgreSQL real). */
@Tag("integration")
class V6MigrationIntegrationTest {

    private static final String URL = env("DB_TEST_URL", "jdbc:postgresql://localhost:5432/turnos_test");
    private static final String USER = env("DB_USER", "turnos");
    private static final String PASSWORD = env("DB_PASSWORD", "turnos_dev");
    private static final OffsetDateTime DIEZ = OffsetDateTime.of(2030, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC);

    private static Connection conn;
    private long profesional, sillon, prestacion, paciente;

    private static String env(String nombre, String porDefecto) {
        String valor = System.getenv(nombre);
        return valor != null ? valor : porDefecto;
    }

    @BeforeAll
    static void migrar() throws SQLException {
        Flyway.configure().dataSource(URL, USER, PASSWORD).load().migrate();
        conn = DriverManager.getConnection(URL, USER, PASSWORD);
    }

    @AfterAll
    static void cerrar() throws SQLException {
        conn.close();
    }

    @BeforeEach
    void datosFicticios() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("TRUNCATE turno_auditoria, turno, paciente, prestacion, sillon, profesional RESTART IDENTITY CASCADE");
        }
        profesional = insertarId("INSERT INTO profesional(nombre, apellido, matricula) VALUES ('Ana','Ejemplo','MP-0001') RETURNING id");
        sillon = insertarId("INSERT INTO sillon(nombre) VALUES ('Sillón 1') RETURNING id");
        prestacion = insertarId("INSERT INTO prestacion(nombre, duracion_minutos) VALUES ('Consulta', 30) RETURNING id");
        paciente = insertarId("INSERT INTO paciente(nombre, apellido, dni) VALUES ('Paciente','Ficticio','00000001') RETURNING id");
    }

    private long insertarId(String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private long insertarTurno() throws SQLException {
        return insertarTurno(false, null, false, null);
    }

    private long insertarTurno(boolean sobreturno, String motivoSobreturno, boolean extendido, String motivoExtension)
            throws SQLException {
        String sql = "INSERT INTO turno(profesional_id, sillon_id, prestacion_id, paciente_id, inicio, fin, "
                + "fin_con_margen, estado, es_sobreturno, motivo_sobreturno, extendido, motivo_extension) "
                + "VALUES (?,?,?,?,?,?,?,'CONFIRMADO',?,?,?,?) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, profesional);
            ps.setLong(2, sillon);
            ps.setLong(3, prestacion);
            ps.setLong(4, paciente);
            ps.setObject(5, DIEZ);
            ps.setObject(6, DIEZ.plusMinutes(30));
            ps.setObject(7, DIEZ.plusMinutes(40));
            ps.setBoolean(8, sobreturno);
            ps.setString(9, motivoSobreturno);
            ps.setBoolean(10, extendido);
            ps.setString(11, motivoExtension);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private String tipoDe(String columna) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT data_type FROM information_schema.columns WHERE table_name = 'turno' AND column_name = ?")) {
            ps.setString(1, columna);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                return rs.getString(1);
            }
        }
    }

    private void auditoria(long turnoId, String accion) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO turno_auditoria(turno_id, accion, actor, fecha) VALUES (?,?,'recepcion', now())")) {
            ps.setLong(1, turnoId);
            ps.setString(2, accion);
            ps.executeUpdate();
        }
    }

    private static void checkViolation(Throwable e) {
        assertThat(e).isInstanceOf(SQLException.class);
        assertThat(((SQLException) e).getSQLState()).isEqualTo("23514");
    }

    @Test
    @DisplayName("extendido existe con valor por defecto FALSE")
    void extendidoExisteConValorPorDefectoFalso() throws SQLException {
        long id = insertarTurno();
        try (PreparedStatement ps = conn.prepareStatement("SELECT extendido FROM turno WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getBoolean(1)).isFalse();
            }
        }
    }

    @Test
    @DisplayName("motivo_extension es TEXT con CHECK de 1 a 500 caracteres")
    void motivoExtensionEsTextConCheck() throws SQLException {
        assertThat(tipoDe("motivo_extension")).isEqualTo("text");
        insertarTurno(false, null, true, "a".repeat(500));
    }

    @Test
    @DisplayName("extendido TRUE sin motivo, con espacios o con 501 caracteres se rechaza")
    void extensionSinMotivoValidoSeRechaza() {
        assertThatThrownBy(() -> insertarTurno(false, null, true, null))
                .satisfies(V6MigrationIntegrationTest::checkViolation);
        assertThatThrownBy(() -> insertarTurno(false, null, true, "   "))
                .satisfies(V6MigrationIntegrationTest::checkViolation);
        assertThatThrownBy(() -> insertarTurno(false, null, true, "a".repeat(501)))
                .satisfies(V6MigrationIntegrationTest::checkViolation);
    }

    @Test
    @DisplayName("motivo_sobreturno pasa a TEXT con CHECK de 1 a 500 caracteres")
    void motivoSobreturnoEsTextConCheck() throws SQLException {
        assertThat(tipoDe("motivo_sobreturno")).isEqualTo("text");
        insertarTurno(true, "a".repeat(500), false, null);
        assertThatThrownBy(() -> insertarTurno(true, "a".repeat(501), false, null))
                .satisfies(V6MigrationIntegrationTest::checkViolation);
        assertThatThrownBy(() -> insertarTurno(true, "   ", false, null))
                .satisfies(V6MigrationIntegrationTest::checkViolation);
    }

    @Test
    @DisplayName("turno_auditoria acepta SOBRETURNO y EXTENDER sin perder las acciones anteriores")
    void auditoriaAceptaAccionesNuevasYViejas() throws SQLException {
        long turnoId = insertarTurno();
        auditoria(turnoId, "SOBRETURNO");
        auditoria(turnoId, "EXTENDER");
        auditoria(turnoId, "CREAR");
        auditoria(turnoId, "CANCELAR");
        auditoria(turnoId, "REPROGRAMAR");
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(
                "SELECT count(*) FROM turno_auditoria WHERE accion IN ('SOBRETURNO','EXTENDER','CREAR','CANCELAR','REPROGRAMAR')")) {
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(5);
        }
        assertThatThrownBy(() -> auditoria(turnoId, "OTRA"))
                .satisfies(V6MigrationIntegrationTest::checkViolation);
    }

    @Test
    @DisplayName("las dos EXCLUDE USING gist conservan nombre y definición")
    void excludeConservanNombreYDefinicion() throws SQLException {
        String sql = "SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint "
                + "WHERE conname IN ('turno_sin_solapamiento_sillon','turno_sin_solapamiento_profesional') ORDER BY conname";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("turno_sin_solapamiento_profesional");
            assertThat(rs.getString(2)).contains("EXCLUDE USING gist", "profesional_id", "es_sobreturno", "fin_con_margen");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("turno_sin_solapamiento_sillon");
            assertThat(rs.getString(2)).contains("EXCLUDE USING gist", "sillon_id", "fin_con_margen");
            assertThat(rs.next()).isFalse();
        }
    }
}
