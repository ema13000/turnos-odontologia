package ar.turnos.odontologia.agenda.application;

import ar.turnos.odontologia.agenda.domain.Intervalo;
import ar.turnos.odontologia.agenda.domain.reglas.PeriodosProfesional;

/**
 * Puerto de salida (C-07, design D1): las franjas semanales del profesional y sus ausencias, licencias y bloqueos
 * que se cruzan con {@code ventana}. Lo implementa la infraestructura de {@code agenda/disponibilidad}.
 */
public interface DisponibilidadProfesional {

    /** Disponibilidad que no limita nada: la usan los constructores anteriores a C-07 (design D2). */
    DisponibilidadProfesional SIN_RESTRICCIONES = (profesionalId, ventana) -> PeriodosProfesional.sinRestricciones();

    PeriodosProfesional periodos(Long profesionalId, Intervalo ventana);
}
