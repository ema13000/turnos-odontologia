package ar.turnos.odontologia.configuracion.application;

/**
 * Puerto: si un profesional está habilitado para una prestación según sus especialidades
 * (RN-AG-15). C-06 y C-10 lo usan al crear turnos y al calcular disponibilidad.
 */
public interface HabilitacionProfesional {

    boolean estaHabilitado(Long profesionalId, Long prestacionId);
}
