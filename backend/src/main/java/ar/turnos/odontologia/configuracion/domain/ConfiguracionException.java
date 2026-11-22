package ar.turnos.odontologia.configuracion.domain;

import java.util.Objects;

/** Error de negocio o de validación de la configuración del consultorio, con código estable. */
public class ConfiguracionException extends RuntimeException {

    private final CodigoError codigo;

    public ConfiguracionException(CodigoError codigo, String mensaje) {
        super(mensaje);
        this.codigo = Objects.requireNonNull(codigo, "codigo");
    }

    public CodigoError codigo() {
        return codigo;
    }

    public static ConfiguracionException noEncontrado(String recurso, Long id) {
        return new ConfiguracionException(CodigoError.RECURSO_NO_ENCONTRADO,
                "No existe " + recurso + " con id " + id);
    }
}
