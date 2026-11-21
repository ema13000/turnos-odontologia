package ar.turnos.odontologia.agenda.domain;

import java.time.Duration;
import java.util.Objects;

/**
 * Tiempo libre obligatorio entre turnos que comparten profesional o sillón (RN-AG-04).
 * Es configurable, pero nunca menor a 10 minutos.
 */
public record Margen(Duration valor) {

    public static final Duration MINIMO = Duration.ofMinutes(10);
    public static final Margen PREDETERMINADO = new Margen(MINIMO);

    public Margen {
        Objects.requireNonNull(valor, "valor");
        if (valor.compareTo(MINIMO) < 0) {
            throw new TurnoRechazadoException(CodigoRechazo.MARGEN_INVALIDO,
                    "El margen entre turnos no puede ser menor a 10 minutos");
        }
    }

    public static Margen deMinutos(int minutos) {
        return new Margen(Duration.ofMinutes(minutos));
    }
}
