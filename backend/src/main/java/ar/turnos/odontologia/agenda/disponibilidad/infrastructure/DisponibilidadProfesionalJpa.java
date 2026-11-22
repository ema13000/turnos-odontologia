package ar.turnos.odontologia.agenda.disponibilidad.infrastructure;

import ar.turnos.odontologia.agenda.application.DisponibilidadProfesional;
import ar.turnos.odontologia.agenda.domain.Intervalo;
import ar.turnos.odontologia.agenda.domain.reglas.FranjaHoraria;
import ar.turnos.odontologia.agenda.domain.reglas.HorarioSemanal;
import ar.turnos.odontologia.agenda.domain.reglas.PeriodoSinAtencion;
import ar.turnos.odontologia.agenda.domain.reglas.PeriodosProfesional;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/** Adaptador del puerto {@link DisponibilidadProfesional}: lee franjas, ausencias y bloqueos de la base (D1). */
@Component
class DisponibilidadProfesionalJpa implements DisponibilidadProfesional {

    private final HorarioTrabajoJpaRepository horarios;
    private final AusenciaProfesionalJpaRepository ausencias;
    private final BloqueoAgendaJpaRepository bloqueos;

    DisponibilidadProfesionalJpa(HorarioTrabajoJpaRepository horarios, AusenciaProfesionalJpaRepository ausencias,
                                 BloqueoAgendaJpaRepository bloqueos) {
        this.horarios = horarios;
        this.ausencias = ausencias;
        this.bloqueos = bloqueos;
    }

    @Override
    public PeriodosProfesional periodos(Long profesionalId, Intervalo ventana) {
        List<FranjaHoraria> franjas = horarios.findByProfesionalIdOrderByDiaSemanaAscHoraDesdeAsc(profesionalId)
                .stream()
                .map(h -> FranjaHoraria.de(h.diaSemana(), h.horaDesde(), h.horaHasta()))
                .toList();
        Stream<PeriodoSinAtencion> deAusencias = ausencias
                .queSeCruzan(profesionalId, ventana.inicio(), ventana.fin()).stream()
                .map(a -> PeriodoSinAtencion.de(PeriodoSinAtencion.Tipo.valueOf(a.tipo()), a.inicio(), a.fin()));
        Stream<PeriodoSinAtencion> deBloqueos = bloqueos
                .queSeCruzan(profesionalId, ventana.inicio(), ventana.fin()).stream()
                .map(b -> PeriodoSinAtencion.de(PeriodoSinAtencion.Tipo.BLOQUEO, b.inicio(), b.fin()));
        return new PeriodosProfesional(HorarioSemanal.de(franjas), Stream.concat(deAusencias, deBloqueos).toList());
    }
}
