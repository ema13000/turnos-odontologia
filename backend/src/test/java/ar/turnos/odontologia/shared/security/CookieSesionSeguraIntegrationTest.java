package ar.turnos.odontologia.shared.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** Cookie de sesión con SESSION_COOKIE_SECURE=true (producción). Clase aparte: cambia la configuración. */
@Tag("integration")
@TestPropertySource(properties = "server.servlet.session.cookie.secure=true")
class CookieSesionSeguraIntegrationTest extends IntegracionServidorBase {

    @Test
    @DisplayName("Cookie segura en producción")
    void cookieSeguraEnProduccion() throws Exception {
        String clave = java.util.UUID.randomUUID().toString();
        crearPersonal("recepcion", "RECEPCIONISTA", clave);

        ResponseEntity<String> respuesta = iniciarSesion("recepcion", clave);

        String cookie = lineaDeCookie(respuesta.getHeaders().get(HttpHeaders.SET_COOKIE), "JSESSIONID");
        assertThat(cookie).contains("Secure").contains("HttpOnly").contains("SameSite=Lax");
    }
}
