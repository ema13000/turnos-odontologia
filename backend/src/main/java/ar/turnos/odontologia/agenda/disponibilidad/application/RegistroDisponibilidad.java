package ar.turnos.odontologia.agenda.disponibilidad.application;

import ar.turnos.odontologia.agenda.domain.reglas.FranjaHoraria;
import ar.turnos.odontologia.agenda.domain.reglas.PeriodoSinAtencion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Puerto de salida: almacenamiento de franjas, ausencias y bloqueos (C-07, design D5 y D7). */
public interface RegistroDisponibilidad {

    boolean existeProfesional(Long profesionalId);

    /** Franjas del profesional ordenadas por día y hora de inicio. */
    List<FranjaHoraria> franjas(Long profesionalId);

    /** Borra las franjas del profesional e inserta {@code franjas}, en la transacción en curso. */
    void reemplazarFranjas(Long profesionalId, List<FranjaHoraria> franjas);

    /** Guarda una ausencia, una licencia o un bloqueo, según {@code nuevo.tipo()}. */
    Periodo registrar(NuevoPeriodo nuevo);

    /** Ausencias y licencias, o bloqueos, del profesional, ordenados por inicio. */
    List<Periodo> periodos(Long profesionalId, Clase clase);

    /** El período con ese id, solo si es de esa clase y de ese profesional. */
    Optional<Periodo> periodo(Long profesionalId, Clase clase, Long periodoId);

    void borrar(Clase clase, Long periodoId);

    /** Las ausencias y licencias van en una tabla y los bloqueos en otra, con permisos distintos (design D5). */
    enum Clase {
        AUSENCIA,
        BLOQUEO
    }

    record NuevoPeriodo(Long profesionalId, PeriodoSinAtencion periodo, String motivo, String registradoPor,
                        Instant registradoEn) {}

    record Periodo(Long id, Long profesionalId, PeriodoSinAtencion periodo, String motivo) {}
}
