package ar.turnos.odontologia.agenda.disponibilidad.api;

import ar.turnos.odontologia.agenda.disponibilidad.application.RegistroDisponibilidad.Periodo;
import ar.turnos.odontologia.agenda.disponibilidad.application.ServicioPeriodos.PeriodoRegistrado;
import ar.turnos.odontologia.agenda.disponibilidad.application.Solicitante;
import ar.turnos.odontologia.agenda.domain.Turno;
import ar.turnos.odontologia.agenda.domain.reglas.CodigoDisponibilidad;
import ar.turnos.odontologia.agenda.domain.reglas.DisponibilidadException;
import ar.turnos.odontologia.agenda.domain.reglas.FranjaHoraria;
import ar.turnos.odontologia.shared.security.UsuarioAutenticado;
import ar.turnos.odontologia.usuarios.domain.Rol;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/** Pedidos y respuestas de la API de disponibilidad (C-07). Records, nunca entidades JPA (regla dura 9). */
final class DisponibilidadDtos {

    private DisponibilidadDtos() {}

    static Solicitante solicitante(UsuarioAutenticado usuario) {
        return new Solicitante(usuario.getUsername(), usuario.rol() == Rol.ODONTOLOGO, usuario.profesionalId());
    }

    static OffsetDateTime local(Instant instante, ZoneId zona) {
        return instante.atZone(zona).toOffsetDateTime();
    }

    /** Una franja semanal: {@code dia} de 1 (lunes) a 7 (domingo), horas {@code HH:mm} locales. */
    record FranjaDto(
            Integer dia,
            @JsonFormat(pattern = "HH:mm") LocalTime desde,
            @JsonFormat(pattern = "HH:mm") LocalTime hasta) {

        static FranjaDto de(FranjaHoraria franja) {
            return new FranjaDto(franja.dia().getValue(), franja.desde(), franja.hasta());
        }

        /** @throws DisponibilidadException {@code HORARIO_INVALIDO} si falta un dato o no es válido */
        FranjaHoraria aDominio() {
            if (dia == null) {
                throw new DisponibilidadException(CodigoDisponibilidad.HORARIO_INVALIDO,
                        "El día de la semana es obligatorio");
            }
            return FranjaHoraria.de(dia, desde, hasta);
        }
    }

    record HorariosSolicitud(@NotNull List<@NotNull @Valid FranjaDto> franjas) {}

    record HorariosRespuesta(Long profesionalId, List<FranjaDto> franjas) {

        static HorariosRespuesta de(Long profesionalId, List<FranjaHoraria> franjas) {
            return new HorariosRespuesta(profesionalId, franjas.stream().map(FranjaDto::de).toList());
        }
    }

    enum TipoAusencia {
        AUSENCIA,
        LICENCIA
    }

    record AusenciaSolicitud(
            @NotNull TipoAusencia tipo,
            @NotNull OffsetDateTime inicio,
            @NotNull OffsetDateTime fin,
            @Size(max = 255) String motivo) {}

    record BloqueoSolicitud(
            @NotNull OffsetDateTime inicio,
            @NotNull OffsetDateTime fin,
            @Size(max = 255) String motivo) {}

    record PeriodoRespuesta(Long id, Long profesionalId, String tipo, OffsetDateTime inicio, OffsetDateTime fin,
                            String motivo) {

        static PeriodoRespuesta de(Periodo p, ZoneId zona) {
            return new PeriodoRespuesta(p.id(), p.profesionalId(), p.periodo().tipo().name(),
                    local(p.periodo().inicio(), zona), local(p.periodo().fin(), zona), p.motivo());
        }
    }

    /** Un turno afectado, sin datos del paciente (design D6). */
    record TurnoAfectado(Long turnoId, OffsetDateTime inicio, OffsetDateTime fin, Long prestacionId) {

        static TurnoAfectado de(Turno t, ZoneId zona) {
            return new TurnoAfectado(t.id(), local(t.intervalo().inicio(), zona), local(t.intervalo().fin(), zona),
                    t.prestacionId());
        }
    }

    /** Destino de una reasignación: lo que falta se conserva del turno original (design D10). */
    record ReasignacionSolicitud(Long profesionalId, OffsetDateTime inicio, Long sillonId) {}

    /** El turno nuevo de una reasignación, con su número de reserva para dárselo al paciente. */
    record TurnoReasignado(Long turnoId, Long profesionalId, Long sillonId, Long prestacionId, OffsetDateTime inicio,
                           OffsetDateTime fin, String estado, String numeroReserva) {

        static TurnoReasignado de(Turno t, ZoneId zona) {
            return new TurnoReasignado(t.id(), t.profesionalId(), t.sillonId(), t.prestacionId(),
                    local(t.intervalo().inicio(), zona), local(t.intervalo().fin(), zona), t.estado().name(),
                    t.numeroReserva().valor());
        }
    }

    record PeriodoRegistradoRespuesta(PeriodoRespuesta periodo, List<TurnoAfectado> turnosAfectados) {

        static PeriodoRegistradoRespuesta de(PeriodoRegistrado registrado, ZoneId zona) {
            return new PeriodoRegistradoRespuesta(PeriodoRespuesta.de(registrado.periodo(), zona),
                    registrado.turnosAfectados().stream().map(t -> TurnoAfectado.de(t, zona)).toList());
        }
    }
}
