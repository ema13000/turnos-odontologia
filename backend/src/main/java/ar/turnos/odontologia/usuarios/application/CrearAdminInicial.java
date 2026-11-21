package ar.turnos.odontologia.usuarios.application;

import ar.turnos.odontologia.usuarios.domain.NombreUsuario;
import ar.turnos.odontologia.usuarios.domain.PoliticaContrasena;
import ar.turnos.odontologia.usuarios.domain.Rol;
import ar.turnos.odontologia.usuarios.domain.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea el primer ADMIN desde la configuración del entorno. No hace nada si ya existe un ADMIN o si falta el
 * usuario o la contraseña, así nunca modifica usuarios existentes.
 */
@Service
public class CrearAdminInicial {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public CrearAdminInicial(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Transactional
    public void ejecutar(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            return;
        }
        if (usuarios.existeConRol(Rol.ADMIN)) {
            return;
        }
        if (!PoliticaContrasena.cumple(password)) {
            // sin incluir la contraseña en el mensaje
            throw new IllegalStateException("La contraseña del administrador inicial no cumple la política de contraseñas");
        }
        usuarios.guardar(new Usuario(null, NombreUsuario.de(username), encoder.encode(password), Rol.ADMIN,
                null, null, true));
    }
}
