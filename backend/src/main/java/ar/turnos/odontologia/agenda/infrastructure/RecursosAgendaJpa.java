package ar.turnos.odontologia.agenda.infrastructure;

import ar.turnos.odontologia.agenda.application.RecursosAgenda;
import ar.turnos.odontologia.shared.persistence.PacienteEntity;
import ar.turnos.odontologia.shared.persistence.PrestacionEntity;
import ar.turnos.odontologia.shared.persistence.ProfesionalEntity;
import ar.turnos.odontologia.shared.persistence.SillonEntity;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adaptador JPA de {@link RecursosAgenda} sobre las tablas de C-03. */
@Repository
class RecursosAgendaJpa implements RecursosAgenda {

    private final EntityManager em;

    RecursosAgendaJpa(EntityManager em) {
        this.em = em;
    }

    @Override
    public boolean existePaciente(Long pacienteId) {
        return em.find(PacienteEntity.class, pacienteId) != null;
    }

    @Override
    public Optional<Boolean> profesionalActivo(Long profesionalId) {
        return Optional.ofNullable(em.find(ProfesionalEntity.class, profesionalId)).map(ProfesionalEntity::isActivo);
    }

    @Override
    public Optional<Boolean> sillonActivo(Long sillonId) {
        return Optional.ofNullable(em.find(SillonEntity.class, sillonId)).map(SillonEntity::isActivo);
    }

    @Override
    public Optional<Prestacion> prestacion(Long prestacionId) {
        return Optional.ofNullable(em.find(PrestacionEntity.class, prestacionId))
                .map(p -> new Prestacion(Duration.ofMinutes(p.getDuracionMinutos()), p.isActivo()));
    }
}
