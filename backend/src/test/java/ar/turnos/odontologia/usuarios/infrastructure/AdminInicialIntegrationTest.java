package ar.turnos.odontologia.usuarios.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Primer arranque con APP_ADMIN_USERNAME y APP_ADMIN_PASSWORD ficticios: el admin creado puede iniciar sesión. */
@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.seguridad.admin-inicial.username=admin",
        "app.seguridad.admin-inicial.password=" + AdminInicialIntegrationTest.CLAVE
})
class AdminInicialIntegrationTest {

    static final String CLAVE = "clave-admin-ficticia-1";

    private static String env(String nombre, String porDefecto) {
        String valor = System.getenv(nombre);
        return valor != null ? valor : porDefecto;
    }

    /** Corre antes de que Spring cree el contexto: el arranque solo crea el admin si la base no tiene ninguno. */
    @BeforeAll
    static void baseSinUsuarios() throws SQLException {
        String url = env("DB_TEST_URL", "jdbc:postgresql://localhost:5432/turnos_test");
        String usuario = env("DB_USER", "turnos");
        String clave = env("DB_PASSWORD", "turnos_dev");
        Flyway.configure().dataSource(url, usuario, clave).outOfOrder(true).load().migrate();
        try (Connection conn = DriverManager.getConnection(url, usuario, clave); Statement st = conn.createStatement()) {
            // OJO: vacía las tablas de la base de tests (DB_TEST_URL)
            st.execute("TRUNCATE usuario, turno, paciente, prestacion, sillon, profesional RESTART IDENTITY CASCADE");
        }
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;

    @Test
    @DisplayName("El administrador inicial creado al arrancar puede iniciar sesión")
    void elAdministradorInicialPuedeIniciarSesion() throws Exception {
        String csrf = mvc.perform(get("/api/v1/public/sesion")).andReturn().getResponse().getCookie("XSRF-TOKEN").getValue();

        mvc.perform(post("/api/v1/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie("XSRF-TOKEN", csrf))
                        .header("X-XSRF-TOKEN", csrf)
                        .content(json.writeValueAsString(Map.of("username", "admin", "password", CLAVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario WHERE rol = 'ADMIN'", Integer.class)).isEqualTo(1);
    }
}
