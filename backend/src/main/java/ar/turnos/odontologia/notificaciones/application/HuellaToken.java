package ar.turnos.odontologia.notificaciones.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Huella SHA-256 hexadecimal del token: lo único que se guarda, así el token del link no se puede leer de la base. */
public final class HuellaToken {

    private HuellaToken() {}

    public static String de(String token) {
        try {
            byte[] huella = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(huella);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 siempre está disponible", e);
        }
    }
}
