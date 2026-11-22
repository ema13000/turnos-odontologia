package ar.turnos.odontologia.agenda.disponibilidad.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface HorarioTrabajoJpaRepository extends JpaRepository<HorarioTrabajoEntity, Long> {

    List<HorarioTrabajoEntity> findByProfesionalIdOrderByDiaSemanaAscHoraDesdeAsc(Long profesionalId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from HorarioTrabajoEntity h where h.profesionalId = :profesionalId")
    void borrarDelProfesional(Long profesionalId);
}
