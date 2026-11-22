package ar.turnos.odontologia.agenda.disponibilidad.infrastructure;

import ar.turnos.odontologia.agenda.disponibilidad.application.RegistroDisponibilidad;
import ar.turnos.odontologia.agenda.domain.reglas.FranjaHoraria;
import ar.turnos.odontologia.agenda.domain.reglas.PeriodoSinAtencion;
import ar.turnos.odontologia.shared.persistence.ProfesionalEntity;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adaptador JPA de {@link RegistroDisponibilidad}. */
@Repository
class RegistroDisponibilidadJpa implements RegistroDisponibilidad {

    private final EntityManager em;
    private final HorarioTrabajoJpaRepository horarios;
    private final AusenciaProfesionalJpaRepository ausencias;
    private final BloqueoAgendaJpaRepository bloqueos;

    RegistroDisponibilidadJpa(EntityManager em, HorarioTrabajoJpaRepository horarios,
                              AusenciaProfesionalJpaRepository ausencias, BloqueoAgendaJpaRepository bloqueos) {
        this.em = em;
        this.horarios = horarios;
        this.ausencias = ausencias;
        this.bloqueos = bloqueos;
    }

    @Override
    public boolean existeProfesional(Long profesionalId) {
        return em.find(ProfesionalEntity.class, profesionalId) != null;
    }

    @Override
    public List<FranjaHoraria> franjas(Long profesionalId) {
        return horarios.findByProfesionalIdOrderByDiaSemanaAscHoraDesdeAsc(profesionalId).stream()
                .map(h -> FranjaHoraria.de(h.diaSemana(), h.horaDesde(), h.horaHasta()))
                .toList();
    }

    @Override
    public void reemplazarFranjas(Long profesionalId, List<FranjaHoraria> franjas) {
        horarios.borrarDelProfesional(profesionalId);
        horarios.saveAllAndFlush(franjas.stream()
                .map(f -> new HorarioTrabajoEntity(profesionalId, f.dia().getValue(), f.desde(), f.hasta()))
                .toList());
    }

    @Override
    public Periodo registrar(NuevoPeriodo nuevo) {
        PeriodoSinAtencion p = nuevo.periodo();
        if (p.tipo() == PeriodoSinAtencion.Tipo.BLOQUEO) {
            return aPeriodo(bloqueos.saveAndFlush(new BloqueoAgendaEntity(nuevo.profesionalId(), p.inicio(),
                    p.fin(), nuevo.motivo(), nuevo.registradoPor(), nuevo.registradoEn())));
        }
        return aPeriodo(ausencias.saveAndFlush(new AusenciaProfesionalEntity(nuevo.profesionalId(),
                p.tipo().name(), p.inicio(), p.fin(), nuevo.motivo(), nuevo.registradoPor(),
                nuevo.registradoEn())));
    }

    @Override
    public List<Periodo> periodos(Long profesionalId, Clase clase) {
        return clase == Clase.AUSENCIA
                ? ausencias.findByProfesionalIdOrderByInicioAsc(profesionalId).stream().map(this::aPeriodo).toList()
                : bloqueos.findByProfesionalIdOrderByInicioAsc(profesionalId).stream().map(this::aPeriodo).toList();
    }

    @Override
    public Optional<Periodo> periodo(Long profesionalId, Clase clase, Long periodoId) {
        Optional<Periodo> periodo = clase == Clase.AUSENCIA
                ? ausencias.findById(periodoId).map(this::aPeriodo)
                : bloqueos.findById(periodoId).map(this::aPeriodo);
        return periodo.filter(p -> p.profesionalId().equals(profesionalId));
    }

    @Override
    public void borrar(Clase clase, Long periodoId) {
        if (clase == Clase.AUSENCIA) {
            ausencias.deleteById(periodoId);
        } else {
            bloqueos.deleteById(periodoId);
        }
    }

    private Periodo aPeriodo(AusenciaProfesionalEntity a) {
        return new Periodo(a.id(), a.profesionalId(),
                PeriodoSinAtencion.de(PeriodoSinAtencion.Tipo.valueOf(a.tipo()), a.inicio(), a.fin()), a.motivo());
    }

    private Periodo aPeriodo(BloqueoAgendaEntity b) {
        return new Periodo(b.id(), b.profesionalId(),
                PeriodoSinAtencion.de(PeriodoSinAtencion.Tipo.BLOQUEO, b.inicio(), b.fin()), b.motivo());
    }
}
