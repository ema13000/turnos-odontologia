package ar.turnos.odontologia.configuracion.infrastructure;

import ar.turnos.odontologia.shared.persistence.PrestacionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de prestaciones del módulo de configuración (distinto del puerto de agenda de C-01). */
public interface PrestacionRepository extends JpaRepository<PrestacionEntity, Long> {

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, Long id);

    List<PrestacionEntity> findAllByOrderByNombreAsc();
}
