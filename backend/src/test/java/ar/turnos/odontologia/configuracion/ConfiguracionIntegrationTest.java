package ar.turnos.odontologia.configuracion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Base de los tests de integración de C-04: entran por la API REST contra PostgreSQL real
 * (perfil {@code test}, base aparte de la de desarrollo). Solo datos ficticios.
 *
 * <p>Desde C-05 la API exige sesión y CSRF: por defecto cada test entra como {@code admin}
 * (rol {@code ADMIN}) y los pedidos que modifican llevan el token CSRF como el frontend
 * (cookie {@code XSRF-TOKEN} y encabezado {@code X-XSRF-TOKEN} iguales). No se usa el atajo
 * {@code csrf()} de spring-security-test porque reemplaza el repositorio CSRF del filtro compartido
 * y rompe los tests de C-05 que corren después en el mismo contexto. Un test puede pedir
 * otro rol con su propio {@link WithMockUser}.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "ADMIN")
public abstract class ConfiguracionIntegrationTest {

    protected static final String API = "/api/v1";

    /** Token CSRF como lo manda el frontend: el mismo valor en la cookie y en el encabezado. */
    private static final RequestPostProcessor CSRF = pedido -> {
        String token = UUID.randomUUID().toString();
        pedido.setCookies(new Cookie("XSRF-TOKEN", token));
        pedido.addHeader("X-XSRF-TOKEN", token);
        return pedido;
    };

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected ObjectMapper json;

    @BeforeEach
    void vaciarTablas() {
        jdbc.execute("TRUNCATE turno, paciente, profesional_prestacion, profesional, prestacion, sillon "
                + "RESTART IDENTITY CASCADE");
        jdbc.update("UPDATE configuracion SET valor = '10' WHERE clave = 'margen_minutos'");
    }

    // ---------- Ayudas ----------

    protected ResultActions postJson(String ruta, String cuerpo) throws Exception {
        return mvc.perform(post(API + ruta).with(CSRF).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    protected ResultActions putJson(String ruta, String cuerpo) throws Exception {
        return mvc.perform(put(API + ruta).with(CSRF).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    protected ResultActions postVacio(String ruta) throws Exception {
        return mvc.perform(post(API + ruta).with(CSRF));
    }

    protected ResultActions obtener(String ruta) throws Exception {
        return mvc.perform(get(API + ruta));
    }

    protected long crearProfesional(String nombre, String apellido, String matricula) throws Exception {
        return id(postJson("/profesionales", """
                {"nombre":"%s","apellido":"%s","matricula":"%s"}""".formatted(nombre, apellido, matricula)));
    }

    protected long crearSillon(String nombre) throws Exception {
        return id(postJson("/sillones", """
                {"nombre":"%s"}""".formatted(nombre)));
    }

    protected long crearPrestacion(String nombre, int minutos) throws Exception {
        return id(postJson("/prestaciones", """
                {"nombre":"%s","duracionMinutos":%d}""".formatted(nombre, minutos)));
    }

    private long id(ResultActions resultado) throws Exception {
        String cuerpo = resultado.andReturn().getResponse().getContentAsString();
        JsonNode nodo = json.readTree(cuerpo);
        return nodo.get("id").asLong();
    }
}
