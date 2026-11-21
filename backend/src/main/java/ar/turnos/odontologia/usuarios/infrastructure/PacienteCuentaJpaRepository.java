package ar.turnos.odontologia.usuarios.infrastructure;

import ar.turnos.odontologia.shared.persistence.PacienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

interface PacienteCuentaJpaRepository extends JpaRepository<PacienteEntity, Long> {

    boolean existsByDni(String dni);
}
