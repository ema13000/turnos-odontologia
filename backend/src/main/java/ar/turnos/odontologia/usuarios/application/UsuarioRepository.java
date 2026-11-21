package ar.turnos.odontologia.usuarios.application;

import ar.turnos.odontologia.usuarios.domain.NombreUsuario;
import ar.turnos.odontologia.usuarios.domain.Rol;
import ar.turnos.odontologia.usuarios.domain.Usuario;

import java.util.Optional;

/** Puerto de persistencia de usuarios. */
public interface UsuarioRepository {

    Optional<Usuario> buscarPorUsername(NombreUsuario username);

    boolean existePorUsername(NombreUsuario username);

    boolean existeConRol(Rol rol);

    /** Guarda un usuario nuevo y lo devuelve con su id. */
    Usuario guardar(Usuario usuario);
}
