package ar.turnos.odontologia.agenda.api;

import ar.turnos.odontologia.agenda.application.CancelarTurnoCommand;
import ar.turnos.odontologia.agenda.application.CrearTurnoCommand;
import ar.turnos.odontologia.agenda.application.ReprogramarTurnoCommand;
import ar.turnos.odontologia.agenda.infrastructure.ServicioAgenda;
import ar.turnos.odontologia.shared.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API de turnos para el personal del consultorio (US-011, US-012). */
@RestController
@RequestMapping("/api/v1/turnos")
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ODONTOLOGO')")
class TurnosController {

    private final ServicioAgenda agenda;
    private final ZoneId zona;

    TurnosController(ServicioAgenda agenda, ZoneId zona) {
        this.agenda = agenda;
        this.zona = zona;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TurnoResponse crear(@Valid @RequestBody CrearTurnoRequest pedido,
                        @AuthenticationPrincipal UsuarioAutenticado usuario) {
        CrearTurnoCommand comando = new CrearTurnoCommand(pedido.pacienteId(), pedido.profesionalId(),
                pedido.sillonId(), pedido.prestacionId(), pedido.inicio().toInstant());
        return TurnoResponse.de(agenda.crear(comando, usuario.getUsername()), zona);
    }

    @GetMapping
    List<TurnoAgendaResponse> agenda(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @RequestParam(required = false) Long profesionalId,
            @RequestParam(required = false) Long sillonId) {
        if (!hasta.isAfter(desde)) {
            throw new PeriodoInvalidoException();
        }
        return agenda.agenda(desde.toInstant(), hasta.toInstant(), profesionalId, sillonId).stream()
                .map(t -> TurnoAgendaResponse.de(t, zona))
                .toList();
    }

    @PostMapping("/{id}/cancelar")
    TurnoResponse cancelar(@PathVariable Long id, @Valid @RequestBody CancelarTurnoRequest pedido,
                           @AuthenticationPrincipal UsuarioAutenticado usuario) {
        CancelarTurnoCommand comando = new CancelarTurnoCommand(id, pedido.dni(), pedido.numeroReserva());
        return TurnoResponse.de(agenda.cancelar(comando, usuario.getUsername()), zona);
    }

    @PostMapping("/{id}/reprogramar")
    TurnoResponse reprogramar(@PathVariable Long id, @Valid @RequestBody ReprogramarTurnoRequest pedido,
                              @AuthenticationPrincipal UsuarioAutenticado usuario) {
        ReprogramarTurnoCommand comando = new ReprogramarTurnoCommand(id, pedido.dni(), pedido.numeroReserva(),
                pedido.inicio().toInstant(), pedido.profesionalId(), pedido.sillonId());
        return TurnoResponse.de(agenda.reprogramar(comando, usuario.getUsername()), zona);
    }

    /** {@code hasta} no es posterior a {@code desde}. */
    static class PeriodoInvalidoException extends RuntimeException {
        PeriodoInvalidoException() {
            super("El fin del período debe ser posterior al inicio");
        }
    }
}
