package ar.turnos.odontologia.usuarios.domain;

/** Roles del sistema (KB 03_actores_y_roles). El nombre coincide con el valor guardado en usuario.rol. */
public enum Rol {
    ADMIN,
    RECEPCIONISTA,
    ODONTOLOGO,
    PACIENTE;

    public boolean esPersonal() {
        return this != PACIENTE;
    }
}
