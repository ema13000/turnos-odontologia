package ar.turnos.odontologia.agenda.disponibilidad.infrastructure;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface BloqueoAgendaJpaRepository extends JpaRepository<BloqueoAgendaEntity, Long> {

    @Query("select b from BloqueoAgendaEntity b where b.profesionalId = :profesionalId"
            + " and b.inicio < :hasta and :desde < b.fin order by b.inicio asc")
    List<BloqueoAgendaEntity> queSeCruzan(Long profesionalId, Instant desde, Instant hasta);

    List<BloqueoAgendaEntity> findByProfesionalIdOrderByInicioAsc(Long profesionalId);
}
