package ar.turnos.odontologia.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Base de los tests con servidor real: MockMvc no emite los atributos de la cookie de sesión
 * (HttpOnly, SameSite, Secure), así que se prueban por HTTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegracionServidorBase {

    @LocalServerPort int puerto;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper json;

    @BeforeEach
    void limpiarDatos() {
        jdbc.execute("TRUNCATE usuario, turno, paciente, prestacion, sillon, profesional RESTART IDENTITY CASCADE");
    }

    protected RestClient cliente() {
        return RestClient.builder().baseUrl("http://localhost:" + puerto).build();
    }

    protected void crearPersonal(String username, String rol, String password) {
        jdbc.update("INSERT INTO usuario(username, password_hash, rol) VALUES (?,?,?)",
                username, passwordEncoder.encode(password), rol);
    }

    /** Inicia sesión como lo hace el frontend y devuelve la respuesta del login. */
    protected ResponseEntity<String> iniciarSesion(String username, String password) throws Exception {
        RestClient cliente = cliente();
        ResponseEntity<String> previa = cliente.get().uri("/api/v1/public/sesion").retrieve().toEntity(String.class);
        String csrf = valorDeCookie(previa.getHeaders().get(HttpHeaders.SET_COOKIE), "XSRF-TOKEN");
        return cliente.post().uri("/api/v1/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Cookie", "XSRF-TOKEN=" + csrf)
                .header("X-XSRF-TOKEN", csrf)
                .body(json.writeValueAsString(Map.of("username", username, "password", password)))
                .retrieve().toEntity(String.class);
    }

    protected static String valorDeCookie(List<String> setCookie, String nombre) {
        return lineaDeCookie(setCookie, nombre).split(";", 2)[0].substring(nombre.length() + 1);
    }

    protected static String lineaDeCookie(List<String> setCookie, String nombre) {
        return setCookie.stream().filter(c -> c.startsWith(nombre + "=")).findFirst()
                .orElseThrow(() -> new AssertionError("No llegó la cookie " + nombre + ": " + setCookie));
    }
}
