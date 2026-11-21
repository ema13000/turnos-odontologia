package ar.turnos.odontologia.agenda.domain;

import java.util.Objects;

/** Un id del pedido no corresponde a un paciente, profesional, sillón o prestación existente. */
public class ReferenciaInexistenteException extends RuntimeException {

    private final String campo;

    public ReferenciaInexistenteException(String campo) {
        super("No existe el recurso indicado en " + campo);
        this.campo = Objects.requireNonNull(campo, "campo");
    }

    /** Nombre del campo del pedido, por ejemplo {@code profesionalId}. */
    public String campo() {
        return campo;
    }
}
