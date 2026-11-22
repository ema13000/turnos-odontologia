package ar.turnos.odontologia.notificaciones.domain;

import java.util.Objects;

/** La confirmación no se pudo hacer. El mensaje nunca incluye el token. */
public class ConfirmacionRechazadaException extends RuntimeException {

    private final CodigoConfirmacion codigo;

    public ConfirmacionRechazadaException(CodigoConfirmacion codigo) {
        super(codigo.name());
        this.codigo = Objects.requireNonNull(codigo, "codigo");
    }

    public CodigoConfirmacion codigo() {
        return codigo;
    }
}
