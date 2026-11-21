package ar.turnos.odontologia.usuarios.domain;

public class RegistroRechazadoException extends RuntimeException {

    private final CodigoRegistro codigo;

    public RegistroRechazadoException(CodigoRegistro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoRegistro codigo() {
        return codigo;
    }
}
