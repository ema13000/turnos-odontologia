package ar.turnos.odontologia.agenda.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Intervalo semiabierto {@code [inicio, fin)}: incluye el inicio y excluye el fin.
 */
public record Intervalo(Instant inicio, Instant fin) {

    public Intervalo {
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fin, "fin");
        if (!fin.isAfter(inicio)) {
            throw new IllegalArgumentException("El fin debe ser posterior al inicio");
        }
    }

    /** Verdadero si los dos intervalos comparten algún instante. */
    public boolean seSuperponeCon(Intervalo otro) {
        return inicio.isBefore(otro.fin) && otro.inicio.isBefore(fin);
    }

    /**
     * Verdadero si entre los dos intervalos no queda libre el margen indicado:
     * {@code this.inicio < otro.fin + margen} y {@code otro.inicio < this.fin + margen}.
     */
    public boolean seAcercaA(Intervalo otro, Margen margen) {
        return inicio.isBefore(otro.fin.plus(margen.valor()))
                && otro.inicio.isBefore(fin.plus(margen.valor()));
    }

    /** El mismo intervalo extendido por el margen antes y después. */
    public Intervalo ampliadoPor(Margen margen) {
        return new Intervalo(inicio.minus(margen.valor()), fin.plus(margen.valor()));
    }
}
