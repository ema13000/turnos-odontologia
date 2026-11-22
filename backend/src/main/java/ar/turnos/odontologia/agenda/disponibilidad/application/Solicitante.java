package ar.turnos.odontologia.agenda.disponibilidad.application;

import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;

/**
 * Quién pide la operación, tomado de la sesión y nunca del pedido. Los roles los controla {@code @PreAuthorize};
 * acá se controla lo que depende del recurso: un odontólogo solo opera sobre su propio profesional (design D8).
 *
 * @param profesionalId profesional vinculado al usuario, o {@code null} si no tiene
 */
public record Solicitante(String usuario, boolean esOdontologo, Long profesionalId) {

    public Solicitante {
        Objects.requireNonNull(usuario, "usuario");
    }

    /** @throws AccessDeniedException si es un odontólogo y {@code profesionalId} no es el suyo (403) */
    void exigirPropioSiEsOdontologo(Long profesionalIdDeLaRuta) {
        if (esOdontologo && !profesionalIdDeLaRuta.equals(profesionalId)) {
            throw new AccessDeniedException("Un odontólogo solo opera sobre su propio profesional");
        }
    }
}
