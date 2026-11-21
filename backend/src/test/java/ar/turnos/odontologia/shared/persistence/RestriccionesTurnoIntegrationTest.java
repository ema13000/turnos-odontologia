package ar.turnos.odontologia.shared.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("integration")
class RestriccionesTurnoIntegrationTest {

    private static final String URL = env("DB_URL", "jdbc:postgresql://localhost:5432/turnos");
    private static final String USER = env("DB_USER", "turnos");
    private static final String PASSWORD = env("DB_PASSWORD", "turnos_dev");

    private static final OffsetDateTime DIEZ = OffsetDateTime.of(2030, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC);

    private static Connection conn;
    private long profA, profB, sillon1, sillon2, prestacion, paciente;

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
        // OJO: vacía las tablas de la base local de desarrollo
        try (Statement st = conn.createStatement()) {
            st.execute("TRUNCATE turno, paciente, prestacion, sillon, profesional RESTART IDENTITY CASCADE");
        }
        profA = insertarId("INSERT INTO profesional(nombre, apellido, matricula) VALUES ('Ana','Ejemplo','MP-0001') RETURNING id");
        profB = insertarId("INSERT INTO profesional(nombre, apellido, matricula) VALUES ('Bruno','Prueba','MP-0002') RETURNING id");
        sillon1 = insertarId("INSERT INTO sillon(nombre) VALUES ('Sillón 1') RETURNING id");
        sillon2 = insertarId("INSERT INTO sillon(nombre) VALUES ('Sillón 2') RETURNING id");
        prestacion = insertarId("INSERT INTO prestacion(nombre, duracion_minutos) VALUES ('Consulta', 30) RETURNING id");
        paciente = insertarId("INSERT INTO paciente(nombre, apellido, dni) VALUES ('Paciente','Ficticio','00000001') RETURNING id");
    }

    private long insertarId(String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    /** Turno de 30 min desde 'inicio' con margen de 10 min (fin_con_margen = fin + 10). */
    private void insertarTurno(long prof, long sillon, OffsetDateTime inicio, String estado,
                               boolean sobreturno, String motivo) throws SQLException {
        String sql = "INSERT INTO turno(profesional_id, sillon_id, prestacion_id, paciente_id, inicio, fin, "
                + "fin_con_margen, estado, es_sobreturno, motivo_sobreturno) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, prof);
            ps.setLong(2, sillon);
            ps.setLong(3, prestacion);
            ps.setLong(4, paciente);
            ps.setObject(5, inicio);
            ps.setObject(6, inicio.plusMinutes(30));
            ps.setObject(7, inicio.plusMinutes(40));
            ps.setString(8, estado);
            ps.setBoolean(9, sobreturno);
            ps.setString(10, motivo);
            ps.executeUpdate();
        }
    }

    private void insertarTurno(long prof, long sillon, OffsetDateTime inicio) throws SQLException {
        insertarTurno(prof, sillon, inicio, "CONFIRMADO", false, null);
    }

    private static void assertSqlState(Throwable e, String esperado) {
        assertThat(e).isInstanceOf(SQLException.class);
        assertThat(((SQLException) e).getSQLState()).isEqualTo(esperado);
    }

    @Test
    void turno_a_las_1030_con_margen_de_10_min_es_rechazado() throws SQLException {
        insertarTurno(profA, sillon1, DIEZ);
        assertThatThrownBy(() -> insertarTurno(profA, sillon1, DIEZ.plusMinutes(30)))
                .satisfies(e -> assertSqlState(e, "23P01"));
    }

    @Test
    void turno_a_las_1040_es_aceptado() throws SQLException {
        insertarTurno(profA, sillon1, DIEZ);
        insertarTurno(profA, sillon1, DIEZ.plusMinutes(40));
    }

    @Test
    void mismo_horario_con_otro_profesional_y_otro_sillon_es_aceptado() throws SQLException {
        insertarTurno(profA, sillon1, DIEZ);
        insertarTurno(profB, sillon2, DIEZ.plusMinutes(30));
    }

    @Test
    void turno_cancelado_no_bloquea_el_horario() throws SQLException {
        insertarTurno(profA, sillon1, DIEZ, "CANCELADO", false, null);
        insertarTurno(profA, sillon1, DIEZ);
    }

    @Test
    void sobreturno_sin_motivo_es_rechazado() {
        assertThatThrownBy(() -> insertarTurno(profA, sillon1, DIEZ, "CONFIRMADO", true, "  "))
                .satisfies(e -> assertSqlState(e, "23514"));
    }

    @Test
    void sobreturno_puede_pisar_al_profesional_pero_no_el_sillon() throws SQLException {
        insertarTurno(profA, sillon1, DIEZ);
        // mismo profesional, otro sillón: permitido
        insertarTurno(profA, sillon2, DIEZ, "CONFIRMADO", true, "Urgencia");
        // mismo sillón: rechazado
        assertThatThrownBy(() -> insertarTurno(profB, sillon1, DIEZ, "CONFIRMADO", true, "Urgencia"))
                .satisfies(e -> assertSqlState(e, "23P01"));
    }

    // Agregados por C-06 (V4): solo RESERVADO y CONFIRMADO ocupan la agenda.

    @Test
    void turno_ausente_no_bloquea_el_horario() throws SQLException {
        insertarTurno(profA, sillon1, DIEZ, "AUSENTE", false, null);
        insertarTurno(profA, sillon1, DIEZ);
    }

    @Test
    void turno_reservado_bloquea_el_horario() throws SQLException {
        insertarTurno(profA, sillon1, DIEZ, "RESERVADO", false, null);
        assertThatThrownBy(() -> insertarTurno(profB, sillon1, DIEZ.plusMinutes(15)))
                .satisfies(e -> assertSqlState(e, "23P01"));
    }
}