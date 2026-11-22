package ar.turnos.odontologia.notificaciones.domain;

/** Códigos estables de rechazo de una confirmación. La API los expone tal cual (404 y 409). */
public enum CodigoConfirmacion {
    TOKEN_INEXISTENTE,
    TOKEN_USADO,
    TURNO_NO_CONFIRMABLE
}
