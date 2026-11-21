package ar.turnos.odontologia.usuarios.domain;

import java.util.Locale;

/** Nombre de usuario normalizado: sin espacios en los extremos y en minúsculas. */
public record NombreUsuario(String valor) {

    public NombreUsuario {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario no puede estar vacío");
        }
        valor = valor.strip().toLowerCase(Locale.ROOT);
    }

    public static NombreUsuario de(String crudo) {
        return new NombreUsuario(crudo);
    }
}
