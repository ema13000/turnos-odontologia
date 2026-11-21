package ar.turnos.odontologia.shared.security;

import ar.turnos.odontologia.usuarios.application.UsuarioRepository;
import ar.turnos.odontologia.usuarios.domain.NombreUsuario;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Adaptador del puerto UsuarioRepository a Spring Security. */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarios;

    public UsuarioDetailsService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        NombreUsuario nombre;
        try {
            nombre = NombreUsuario.de(username);
        } catch (IllegalArgumentException e) {
            throw new UsernameNotFoundException("Usuario inexistente");
        }
        return usuarios.buscarPorUsername(nombre)
                .map(UsuarioAutenticado::de)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario inexistente"));
    }
}
