package ar.turnos.odontologia.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

/** Base de los tests de integración de seguridad: PostgreSQL real, MockMvc y datos ficticios. */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegracionSeguridadBase {

    protected static final String LOGIN = "/api/v1/public/login";
    protected static final String SESION = "/api/v1/public/sesion";
    protected static final String LOGOUT = "/api/v1/logout";

    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected ObjectMapper json;

    @BeforeEach
    void limpiarDatos() {
        // OJO: vacía las tablas de la base de tests (DB_TEST_URL)
        jdbc.execute("TRUNCATE usuario, turno, paciente, prestacion, sillon, profesional RESTART IDENTITY CASCADE");
    }

    protected Navegador navegador() {
        return new Navegador(mvc);
    }

    /** Contraseña ficticia generada para el test (nunca una real). */
    protected static String contrasenaFicticia() {
        return UUID.randomUUID().toString();
    }

    protected String credenciales(String username, String password) throws Exception {
        return json.writeValueAsString(Map.of("username", username, "password", password));
    }

    protected long crearPersonal(String username, String rol, String password, boolean activo) {
        Long profesionalId = null;
        if ("ODONTOLOGO".equals(rol)) {
            profesionalId = jdbc.queryForObject(
                    "INSERT INTO profesional(nombre, apellido, matricula) VALUES ('Ana','Ejemplo','MP-0001') RETURNING id",
                    Long.class);
        }
        return jdbc.queryForObject(
                "INSERT INTO usuario(username, password_hash, rol, profesional_id, activo) VALUES (?,?,?,?,?) RETURNING id",
                Long.class, username, passwordEncoder.encode(password), rol, profesionalId, activo);
    }

    protected long crearPersonal(String username, String rol, String password) {
        return crearPersonal(username, rol, password, true);
    }

    /** Crea un paciente y su cuenta (usuario = email en minúsculas). Devuelve el id del paciente. */
    protected long crearPacienteConCuenta(String nombre, String apellido, String dni, String email, String password) {
        long pacienteId = crearPacienteSinCuenta(nombre, apellido, dni, email);
        jdbc.update("INSERT INTO usuario(username, password_hash, rol, paciente_id) VALUES (?,?,'PACIENTE',?)",
                email.toLowerCase(), passwordEncoder.encode(password), pacienteId);
        return pacienteId;
    }

    protected long crearPacienteSinCuenta(String nombre, String apellido, String dni, String email) {
        return jdbc.queryForObject(
                "INSERT INTO paciente(nombre, apellido, dni, email) VALUES (?,?,?,?) RETURNING id",
                Long.class, nombre, apellido, dni, email);
    }

    /** Pide el token CSRF como el frontend (GET /public/sesion) y inicia sesión con las credenciales. */
    protected Navegador iniciarSesion(String username, String password) throws Exception {
        Navegador navegador = navegador();
        navegador.get(SESION);
        navegador.post(LOGIN, credenciales(username, password));
        return navegador;
    }
}
