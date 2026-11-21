package ar.turnos.odontologia.usuarios.infrastructure;

import ar.turnos.odontologia.usuarios.domain.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {
    Optional<UsuarioEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByRol(Rol rol);
}
