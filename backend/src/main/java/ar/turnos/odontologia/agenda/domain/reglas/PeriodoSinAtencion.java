package ar.turnos.odontologia.agenda.domain.reglas;

import ar.turnos.odontologia.agenda.domain.Intervalo;
import java.time.Instant;
import java.util.Objects;

/**
 * Período en que el profesional no atiende: una ausencia o licencia (RN-AG-16) o un bloqueo de su agenda (RN-AG-10).
 * Ocupa el rango semiabierto {@code [inicio, fin)}. El motivo no se guarda acá: las reglas no lo necesitan.
 */
public record PeriodoSinAtencion(Tipo tipo, Instant inicio, Instant fin) {

    public enum Tipo {
        AUSENCIA,
        LICENCIA,
        BLOQUEO;

        public boolean esAusencia() {
            return this != BLOQUEO;
        }
    }

    public PeriodoSinAtencion {
        Objects.requireNonNull(tipo, "tipo");
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new DisponibilidadException(CodigoDisponibilidad.PERIODO_INVALIDO,
                    "El fin del período tiene que ser posterior al inicio");
        }
    }

    public static PeriodoSinAtencion de(Tipo tipo, Instant inicio, Instant fin) {
        return new PeriodoSinAtencion(tipo, inicio, fin);
    }

    /** Un turno que termina justo cuando empieza el período no se cruza con él (design D3). */
    public boolean seCruzaCon(Intervalo intervalo) {
        return intervalo.inicio().isBefore(fin) && inicio.isBefore(intervalo.fin());
    }
}
