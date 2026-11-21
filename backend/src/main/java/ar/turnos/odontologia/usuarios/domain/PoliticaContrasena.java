package ar.turnos.odontologia.usuarios.domain;

import java.nio.charset.StandardCharsets;

/** Política de contraseñas: de 8 a 72 caracteres (72 es el límite de BCrypt). */
public final class PoliticaContrasena {

    public static final int MINIMO = 8;
    public static final int MAXIMO = 72;

    private PoliticaContrasena() {}

    public static boolean cumple(String contrasena) {
        return contrasena != null
                && contrasena.length() >= MINIMO
                && contrasena.length() <= MAXIMO
                && contrasena.getBytes(StandardCharsets.UTF_8).length <= MAXIMO;
    }
}
