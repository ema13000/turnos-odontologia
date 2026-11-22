package ar.turnos.odontologia.agenda.disponibilidad.api;

import ar.turnos.odontologia.agenda.disponibilidad.api.DisponibilidadDtos.FranjaDto;
import ar.turnos.odontologia.agenda.disponibilidad.api.DisponibilidadDtos.HorariosRespuesta;
import ar.turnos.odontologia.agenda.disponibilidad.api.DisponibilidadDtos.HorariosSolicitud;
import ar.turnos.odontologia.agenda.disponibilidad.application.ServicioHorarios;
import ar.turnos.odontologia.shared.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Horarios de trabajo de un profesional (RN-AG-13). Solo ADMIN los define; ADMIN y la recepción leen los de
 * cualquiera, y un odontólogo solo los suyos (design D8).
 */
@RestController
@RequestMapping("/api/v1/profesionales/{id}/horarios")
class HorariosController {

    private final ServicioHorarios servicio;

    HorariosController(ServicioHorarios servicio) {
        this.servicio = servicio;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ODONTOLOGO')")
    @GetMapping
    HorariosRespuesta consultar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return HorariosRespuesta.de(id, servicio.consultar(id, DisponibilidadDtos.solicitante(usuario)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    HorariosRespuesta definir(@PathVariable Long id, @Valid @RequestBody HorariosSolicitud solicitud) {
        return HorariosRespuesta.de(id, servicio.definir(id,
                solicitud.franjas().stream().map(FranjaDto::aDominio).toList()));
    }
}
