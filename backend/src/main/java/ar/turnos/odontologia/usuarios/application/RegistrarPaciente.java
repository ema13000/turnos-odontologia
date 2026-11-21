package ar.turnos.odontologia.usuarios.application;

import ar.turnos.odontologia.usuarios.domain.CodigoRegistro;
import ar.turnos.odontologia.usuarios.domain.NombreUsuario;
import ar.turnos.odontologia.usuarios.domain.PerfilPaciente;
import ar.turnos.odontologia.usuarios.domain.PoliticaContrasena;
import ar.turnos.odontologia.usuarios.domain.RegistroRechazadoException;
import ar.turnos.odontologia.usuarios.domain.Rol;
import ar.turnos.odontologia.usuarios.domain.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registro del paciente: crea el paciente y su cuenta PACIENTE en una sola transacción (US-024, RN-RO-08). */
@Service
public class RegistrarPaciente {

    /** Datos del registro. {@code toString} no incluye la contraseña. */
    public record Datos(String nombre, String apellido, String dni, String email, String telefono, String password) {
        @Override
        public String toString() {
            return "Datos[dni=" + dni + ", email=" + email + ", password=***]";
        }
    }

    private final UsuarioRepository usuarios;
    private final PacienteRegistro pacientes;
    private final PasswordEncoder encoder;

    public RegistrarPaciente(UsuarioRepository usuarios, PacienteRegistro pacientes, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.pacientes = pacientes;
        this.encoder = encoder;
    }

    @Transactional
    public Usuario registrar(Datos datos) {
        if (!PoliticaContrasena.cumple(datos.password())) {
            throw new IllegalArgumentException("La contraseña no cumple la política");
        }
        NombreUsuario username = NombreUsuario.de(datos.email());
        if (pacientes.existePorDni(datos.dni())) {
            throw new RegistroRechazadoException(CodigoRegistro.DNI_YA_REGISTRADO);
        }
        if (usuarios.existePorUsername(username)) {
            throw new RegistroRechazadoException(CodigoRegistro.EMAIL_YA_REGISTRADO);
        }
        long pacienteId = pacientes.crear(new PerfilPaciente(datos.nombre(), datos.apellido(), datos.dni(),
                username.valor(), datos.telefono()));
        return usuarios.guardar(new Usuario(null, username, encoder.encode(datos.password()), Rol.PACIENTE,
                null, pacienteId, true));
    }
}
