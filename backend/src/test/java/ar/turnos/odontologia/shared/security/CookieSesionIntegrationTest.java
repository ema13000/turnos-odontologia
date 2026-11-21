package ar.turnos.odontologia.shared.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/** Atributos de la cookie de sesión con la configuración por defecto (spec seguridad-autenticacion). */
@Tag("integration")
class CookieSesionIntegrationTest extends IntegracionServidorBase {

    @Test
    @DisplayName("Atributos de la cookie de sesión")
    void atributosDeLaCookieDeSesion() throws Exception {
        String clave = java.util.UUID.randomUUID().toString();
        crearPersonal("recepcion", "RECEPCIONISTA", clave);

        ResponseEntity<String> respuesta = iniciarSesion("recepcion", clave);

        String cookie = lineaDeCookie(respuesta.getHeaders().get(HttpHeaders.SET_COOKIE), "JSESSIONID");
        assertThat(cookie).contains("HttpOnly").contains("SameSite=Lax").doesNotContain("Secure");
        assertThat(respuesta.getBody()).doesNotContain(valorDeCookie(respuesta.getHeaders().get(HttpHeaders.SET_COOKIE), "JSESSIONID"));
        assertThat(respuesta.getBody()).doesNotContainIgnoringCase("token").doesNotContainIgnoringCase("jwt");
    }

    @Test
    @DisplayName("La cookie del token CSRF es legible por el frontend")
    void cookieCsrfLegible() {
        ResponseEntity<String> respuesta = cliente().get().uri("/api/v1/public/sesion").retrieve().toEntity(String.class);

        String cookie = lineaDeCookie(respuesta.getHeaders().get(HttpHeaders.SET_COOKIE), "XSRF-TOKEN");
        assertThat(cookie).doesNotContain("HttpOnly");
    }
}
