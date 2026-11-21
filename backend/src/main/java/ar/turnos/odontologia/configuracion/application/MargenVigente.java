package ar.turnos.odontologia.configuracion.application;

import ar.turnos.odontologia.agenda.domain.Margen;

/**
 * Puerto: margen entre turnos vigente en el consultorio (RN-AG-04). C-06 lo usa para construir
 * {@code CrearTurno} con el valor configurado.
 */
public interface MargenVigente {

    Margen actual();
}
