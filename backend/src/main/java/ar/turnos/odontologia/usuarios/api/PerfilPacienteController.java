package ar.turnos.odontologia.usuarios.api;

import ar.turnos.odontologia.shared.security.UsuarioAutenticado;
import ar.turnos.odontologia.usuarios.application.ConsultarPerfilPaciente;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Datos del propio paciente. Se identifica solo por la sesión: este controller no declara ningún
 * {@code @PathVariable} ni {@code @RequestParam} con ids de paciente.
 */
@RestController
@RequestMapping("/api/v1/paciente")
class PerfilPacienteController {

    private final ConsultarPerfilPaciente consultarPerfil;

    PerfilPacienteController(ConsultarPerfilPaciente consultarPerfil) {
        this.consultarPerfil = consultarPerfil;
    }

    @GetMapping("/perfil")
    PerfilPacienteResponse perfil(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return PerfilPacienteResponse.de(consultarPerfil.de(usuario.pacienteId()));
    }
}
