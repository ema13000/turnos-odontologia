package ar.turnos.odontologia.agenda.domain;

import java.util.Objects;

public class TurnoRechazadoException extends RuntimeException {

    private final CodigoRechazo codigo;

    public TurnoRechazadoException(CodigoRechazo codigo, String mensaje) {
        super(mensaje);
        this.codigo = Objects.requireNonNull(codigo, "codigo");
    }

    public CodigoRechazo codigo() {
        return codigo;
    }
}
