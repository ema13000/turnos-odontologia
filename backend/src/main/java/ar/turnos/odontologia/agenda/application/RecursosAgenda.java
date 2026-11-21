package ar.turnos.odontologia.agenda.application;

import java.time.Duration;
import java.util.Optional;

/**
 * Puerto de salida: los recursos que usa un turno. Un {@link Optional} vacío indica que el recurso no
 * existe (422); el booleano, si está activo (RN-AG-11).
 */
public interface RecursosAgenda {

    boolean existePaciente(Long pacienteId);

    Optional<Boolean> profesionalActivo(Long profesionalId);

    Optional<Boolean> sillonActivo(Long sillonId);

    Optional<Prestacion> prestacion(Long prestacionId);

    /** Datos de la prestación que necesita la agenda. */
    record Prestacion(Duration duracion, boolean activa) {}
}
