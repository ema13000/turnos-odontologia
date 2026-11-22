package ar.turnos.odontologia.agenda.disponibilidad.api;

import ar.turnos.odontologia.agenda.disponibilidad.api.DisponibilidadDtos.BloqueoSolicitud;
import ar.turnos.odontologia.agenda.disponibilidad.api.DisponibilidadDtos.PeriodoRegistradoRespuesta;
import ar.turnos.odontologia.agenda.disponibilidad.api.DisponibilidadDtos.PeriodoRespuesta;
import ar.turnos.odontologia.agenda.disponibilidad.application.RegistroDisponibilidad.Clase;
import ar.turnos.odontologia.agenda.disponibilidad.application.ServicioPeriodos;
import ar.turnos.odontologia.agenda.domain.reglas.PeriodoSinAtencion;
import ar.turnos.odontologia.shared.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bloqueos de agenda (RN-AG-10, US-015). ADMIN opera sobre cualquier profesional y un odontólogo solo sobre el
 * suyo; la recepción no tiene acceso (design D8).
 */
@RestController
@RequestMapping("/api/v1/profesionales/{id}/bloqueos")
@PreAuthorize("hasAnyRole('ADMIN', 'ODONTOLOGO')")
class BloqueosController {

    private final ServicioPeriodos servicio;
    private final ZoneId zona;

    BloqueosController(ServicioPeriodos servicio, ZoneId zona) {
        this.servicio = servicio;
        this.zona = zona;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PeriodoRegistradoRespuesta registrar(@PathVariable Long id, @Valid @RequestBody BloqueoSolicitud solicitud,
                                         @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return PeriodoRegistradoRespuesta.de(servicio.registrar(id, Clase.BLOQUEO, PeriodoSinAtencion.Tipo.BLOQUEO,
                solicitud.inicio().toInstant(), solicitud.fin().toInstant(), solicitud.motivo(),
                DisponibilidadDtos.solicitante(usuario)), zona);
    }

    @GetMapping
    List<PeriodoRespuesta> consultar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.consultar(id, Clase.BLOQUEO, DisponibilidadDtos.solicitante(usuario)).stream()
                .map(p -> PeriodoRespuesta.de(p, zona))
                .toList();
    }

    @DeleteMapping("/{bloqueoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void borrar(@PathVariable Long id, @PathVariable Long bloqueoId,
                @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicio.borrar(id, Clase.BLOQUEO, bloqueoId, DisponibilidadDtos.solicitante(usuario));
    }
}
