package ar.turnos.odontologia.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 403 CSRF_INVALIDO cuando falla el token CSRF y 403 ACCESO_DENEGADO para todo lo demás. */
@Component
class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemDetailWriter escritor;

    ProblemDetailAccessDeniedHandler(ProblemDetailWriter escritor) {
        this.escritor = escritor;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException excepcion) throws IOException {
        if (excepcion instanceof CsrfException) {
            escritor.escribir(response, HttpStatus.FORBIDDEN, "CSRF_INVALIDO", "Token CSRF ausente o inválido");
        } else {
            escritor.escribir(response, HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", "No tenés permiso para esta operación");
        }
    }
}
