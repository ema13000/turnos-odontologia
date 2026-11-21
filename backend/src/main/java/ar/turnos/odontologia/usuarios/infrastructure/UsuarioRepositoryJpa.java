package ar.turnos.odontologia.usuarios.infrastructure;

import ar.turnos.odontologia.usuarios.application.UsuarioRepository;
import ar.turnos.odontologia.usuarios.domain.NombreUsuario;
import ar.turnos.odontologia.usuarios.domain.Rol;
import ar.turnos.odontologia.usuarios.domain.Usuario;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class UsuarioRepositoryJpa implements UsuarioRepository {

    private final UsuarioJpaRepository jpa;

    UsuarioRepositoryJpa(UsuarioJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Usuario> buscarPorUsername(NombreUsuario username) {
        return jpa.findByUsername(username.valor()).map(UsuarioRepositoryJpa::aDominio);
    }

    @Override
    public boolean existePorUsername(NombreUsuario username) {
        return jpa.existsByUsername(username.valor());
    }

    @Override
    public boolean existeConRol(Rol rol) {
        return jpa.existsByRol(rol);
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        UsuarioEntity entidad = new UsuarioEntity(usuario.username().valor(), usuario.passwordHash(),
                usuario.rol(), usuario.profesionalId(), usuario.pacienteId());
        entidad.setActivo(usuario.activo());
        return aDominio(jpa.saveAndFlush(entidad));
    }

    private static Usuario aDominio(UsuarioEntity e) {
        return new Usuario(e.getId(), NombreUsuario.de(e.getUsername()), e.getPasswordHash(), e.getRol(),
                e.getProfesionalId(), e.getPacienteId(), e.isActivo());
    }
}
