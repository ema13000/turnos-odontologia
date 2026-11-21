package ar.turnos.odontologia.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 401 NO_AUTENTICADO en un ProblemDetail, sin redirección ni WWW-Authenticate. */
@Component
class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemDetailWriter escritor;

    ProblemDetailAuthenticationEntryPoint(ProblemDetailWriter escritor) {
        this.escritor = escritor;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        escritor.escribir(response, HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO", "Hay que iniciar sesión");
    }
}
