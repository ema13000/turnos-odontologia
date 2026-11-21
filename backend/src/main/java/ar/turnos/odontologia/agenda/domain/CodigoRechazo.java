package ar.turnos.odontologia.agenda.domain;

/** Códigos estables de rechazo. La API los expone tal cual, como 409 (C-06). */
public enum CodigoRechazo {
    SOLAPAMIENTO_PROFESIONAL,
    SOLAPAMIENTO_SILLON,
    MARGEN_INSUFICIENTE,
    INICIO_EN_EL_PASADO,
    MARGEN_INVALIDO,
    RECURSO_INACTIVO,
    DATOS_DE_RESERVA_INVALIDOS,
    TURNO_NO_ACTIVO
}
