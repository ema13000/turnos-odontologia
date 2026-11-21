package ar.turnos.odontologia.usuarios.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;

import java.sql.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Restricciones de la tabla usuario (V3), probadas con JDBC sobre PostgreSQL real. Solo datos ficticios. */
@Tag("integration")
class UsuarioRestriccionesIntegrationTest {

    private static final String URL = env("DB_URL", "jdbc:postgresql://localhost:5432/turnos");
    private static final String USER = env("DB_USER", "turnos");
    private static final String PASSWORD = env("DB_PASSWORD", "turnos_dev");
    private static final String HASH = "$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUVWXYZ01234";

    private static final String CHECK_VIOLATION = "23514";
    private static final String UNIQUE_VIOLATION = "23505";

    private static Connection conn;
    private long profesional, paciente;

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
            st.execute("TRUNCATE usuario, turno, paciente, prestacion, sillon, profesional RESTART IDENTITY CASCADE");
        }
        profesional = insertarId("INSERT INTO profesional(nombre, apellido, matricula) VALUES ('Ana','Ejemplo','MP-0001') RETURNING id");
        paciente = insertarId("INSERT INTO paciente(nombre, apellido, dni) VALUES ('Paciente','Uno','99000001') RETURNING id");
    }

    private long insertarId(String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private void insertarUsuario(String username, String rol, Long profesionalId, Long pacienteId) throws SQLException {
        String sql = "INSERT INTO usuario(username, password_hash, rol, profesional_id, paciente_id) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, HASH);
            ps.setString(3, rol);
            ps.setObject(4, profesionalId);
            ps.setObject(5, pacienteId);
            ps.executeUpdate();
        }
    }

    private static void assertSqlState(Throwable e, String esperado) {
        assertThat(e).isInstanceOf(SQLException.class);
        assertThat(((SQLException) e).getSQLState()).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Un usuario RECEPCIONISTA sin vínculos es aceptado")
    void recepcionista_valido() throws SQLException {
        insertarUsuario("recepcion", "RECEPCIONISTA", null, null);
    }

    @Test
    @DisplayName("Un usuario ADMIN sin vínculos es aceptado")
    void admin_valido() throws SQLException {
        insertarUsuario("admin", "ADMIN", null, null);
    }

    @Test
    @DisplayName("Un ADMIN puede estar vinculado a un profesional")
    void admin_con_profesional_valido() throws SQLException {
        insertarUsuario("admin", "ADMIN", profesional, null);
    }

    @Test
    @DisplayName("Un ODONTOLOGO con profesional es aceptado")
    void odontologo_valido() throws SQLException {
        insertarUsuario("ana.ejemplo", "ODONTOLOGO", profesional, null);
    }

    @Test
    @DisplayName("Un PACIENTE con paciente es aceptado")
    void paciente_valido() throws SQLException {
        insertarUsuario("paciente.uno@example.com", "PACIENTE", null, paciente);
    }

    @Test
    @DisplayName("El username duplicado es rechazado")
    void username_duplicado() throws SQLException {
        insertarUsuario("recepcion", "RECEPCIONISTA", null, null);
        assertThatThrownBy(() -> insertarUsuario("recepcion", "RECEPCIONISTA", null, null))
                .satisfies(e -> assertSqlState(e, UNIQUE_VIOLATION));
    }

    @Test
    @DisplayName("El username con mayúsculas o espacios es rechazado")
    void username_sin_normalizar() {
        assertThatThrownBy(() -> insertarUsuario("Recepcion", "RECEPCIONISTA", null, null))
                .satisfies(e -> assertSqlState(e, CHECK_VIOLATION));
        assertThatThrownBy(() -> insertarUsuario(" recepcion ", "RECEPCIONISTA", null, null))
                .satisfies(e -> assertSqlState(e, CHECK_VIOLATION));
    }

    @Test
    @DisplayName("Un rol inválido es rechazado")
    void rol_invalido() {
        assertThatThrownBy(() -> insertarUsuario("recepcion", "SUPERUSUARIO", null, null))
                .satisfies(e -> assertSqlState(e, CHECK_VIOLATION));
    }

    @Test
    @DisplayName("Un PACIENTE sin paciente_id es rechazado")
    void paciente_sin_vinculo() {
        assertThatThrownBy(() -> insertarUsuario("paciente.uno@example.com", "PACIENTE", null, null))
                .satisfies(e -> assertSqlState(e, CHECK_VIOLATION));
    }

    @Test
    @DisplayName("Un usuario del personal con paciente_id es rechazado")
    void personal_con_paciente() {
        assertThatThrownBy(() -> insertarUsuario("recepcion", "RECEPCIONISTA", null, paciente))
                .satisfies(e -> assertSqlState(e, CHECK_VIOLATION));
    }

    @Test
    @DisplayName("Un PACIENTE con profesional_id es rechazado")
    void paciente_con_profesional() {
        assertThatThrownBy(() -> insertarUsuario("paciente.uno@example.com", "PACIENTE", profesional, paciente))
                .satisfies(e -> assertSqlState(e, CHECK_VIOLATION));
    }

    @Test
    @DisplayName("Un ODONTOLOGO sin profesional_id es rechazado")
    void odontologo_sin_profesional() {
        assertThatThrownBy(() -> insertarUsuario("ana.ejemplo", "ODONTOLOGO", null, null))
                .satisfies(e -> assertSqlState(e, CHECK_VIOLATION));
    }

    @Test
    @DisplayName("Un RECEPCIONISTA con profesional_id es rechazado")
    void recepcionista_con_profesional() {
        assertThatThrownBy(() -> insertarUsuario("recepcion", "RECEPCIONISTA", profesional, null))
                .satisfies(e -> assertSqlState(e, CHECK_VIOLATION));
    }

    @Test
    @DisplayName("Dos usuarios para el mismo paciente son rechazados")
    void dos_usuarios_mismo_paciente() throws SQLException {
        insertarUsuario("paciente.uno@example.com", "PACIENTE", null, paciente);
        assertThatThrownBy(() -> insertarUsuario("otro.email@example.com", "PACIENTE", null, paciente))
                .satisfies(e -> assertSqlState(e, UNIQUE_VIOLATION));
    }

    @Test
    @DisplayName("Dos usuarios para el mismo profesional son rechazados")
    void dos_usuarios_mismo_profesional() throws SQLException {
        insertarUsuario("ana.ejemplo", "ODONTOLOGO", profesional, null);
        assertThatThrownBy(() -> insertarUsuario("admin", "ADMIN", profesional, null))
                .satisfies(e -> assertSqlState(e, UNIQUE_VIOLATION));
    }
}
