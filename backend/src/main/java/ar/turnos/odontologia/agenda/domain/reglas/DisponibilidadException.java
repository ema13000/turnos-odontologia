package ar.turnos.odontologia.agenda.domain.reglas;

import java.util.Objects;

/** Dato inválido al definir horarios o registrar un período sin atención. */
public class DisponibilidadException extends RuntimeException {

    private final CodigoDisponibilidad codigo;

    public DisponibilidadException(CodigoDisponibilidad codigo, String mensaje) {
        super(mensaje);
        this.codigo = Objects.requireNonNull(codigo, "codigo");
    }

    public CodigoDisponibilidad codigo() {
        return codigo;
    }
}
