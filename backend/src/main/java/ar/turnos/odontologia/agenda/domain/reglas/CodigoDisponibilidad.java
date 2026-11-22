package ar.turnos.odontologia.agenda.domain.reglas;

/** Códigos estables de los datos inválidos de horarios, ausencias y bloqueos (C-07). La API los expone como 422. */
public enum CodigoDisponibilidad {
    HORARIO_INVALIDO,
    FRANJAS_SUPERPUESTAS,
    PERIODO_INVALIDO
}
