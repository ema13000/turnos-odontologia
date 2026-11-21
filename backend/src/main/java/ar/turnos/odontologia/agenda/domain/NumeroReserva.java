package ar.turnos.odontologia.agenda.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Número de reserva del turno: 8 caracteres con el formato {@code XXXX-XXXX}, tomados de un alfabeto
 * sin los caracteres ambiguos {@code 0 O 1 I L}. Identifica al turno ante el paciente, porque los ids
 * son correlativos (DD-12).
 */
public record NumeroReserva(String valor) {

    /** 31 símbolos: letras mayúsculas sin I, L, O y dígitos del 2 al 9. */
    public static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    public static final int LARGO = 8;

    private static final Pattern FORMATO =
            Pattern.compile("^[" + ALFABETO + "]{4}-[" + ALFABETO + "]{4}$");

    public NumeroReserva {
        Objects.requireNonNull(valor, "valor");
        if (!FORMATO.matcher(valor).matches()) {
            throw new IllegalArgumentException("Número de reserva con formato inválido");
        }
    }

    /** Arma el número a partir de 8 símbolos del alfabeto, agregando el guion. */
    public static NumeroReserva deSimbolos(String simbolos) {
        if (simbolos == null || simbolos.length() != LARGO) {
            throw new IllegalArgumentException("Se esperan " + LARGO + " símbolos");
        }
        return new NumeroReserva(simbolos.substring(0, 4) + "-" + simbolos.substring(4));
    }

    /** Compara con un valor ingresado sin filtrar información por el tiempo de respuesta. */
    public boolean coincideCon(String ingresado) {
        if (ingresado == null) {
            return false;
        }
        return MessageDigest.isEqual(valor.getBytes(StandardCharsets.UTF_8),
                ingresado.trim().toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String toString() {
        return valor;
    }
}
