package ar.turnos.odontologia.agenda.disponibilidad.application;

import ar.turnos.odontologia.agenda.application.TurnoRepository;
import ar.turnos.odontologia.agenda.disponibilidad.application.RegistroDisponibilidad.Clase;
import ar.turnos.odontologia.agenda.disponibilidad.application.RegistroDisponibilidad.NuevoPeriodo;
import ar.turnos.odontologia.agenda.disponibilidad.application.RegistroDisponibilidad.Periodo;
import ar.turnos.odontologia.agenda.domain.Intervalo;
import ar.turnos.odontologia.agenda.domain.Turno;
import ar.turnos.odontologia.agenda.domain.reglas.PeriodoSinAtencion;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ausencias y licencias (RN-AG-16) y bloqueos de agenda (RN-AG-10) de un profesional. Registrar un período no
 * toca los turnos ya dados: devuelve los activos que se cruzan con él para que el personal los reasigne
 * (design D6). El motivo puede ser sensible: no se escribe en logs.
 */
@Service
@Transactional
public class ServicioPeriodos {

    private final RegistroDisponibilidad registro;
    private final TurnoRepository turnos;
    private final Clock reloj;

    public ServicioPeriodos(RegistroDisponibilidad registro, TurnoRepository turnos, Clock reloj) {
        this.registro = registro;
        this.turnos = turnos;
        this.reloj = reloj;
    }

    /**
     * @param tipo {@code AUSENCIA} o {@code LICENCIA} para la clase {@code AUSENCIA}; {@code BLOQUEO} para la otra
     * @throws ar.turnos.odontologia.agenda.domain.reglas.DisponibilidadException si el fin no es posterior al inicio
     */
    public PeriodoRegistrado registrar(Long profesionalId, Clase clase, PeriodoSinAtencion.Tipo tipo,
                                       Instant inicio, Instant fin, String motivo, Solicitante solicitante) {
        solicitante.exigirPropioSiEsOdontologo(profesionalId);
        exigirProfesional(profesionalId);
        PeriodoSinAtencion periodo = PeriodoSinAtencion.de(tipo, inicio, fin);
        Periodo guardado = registro.registrar(new NuevoPeriodo(profesionalId, periodo, motivo,
                solicitante.usuario(), reloj.instant()));
        return new PeriodoRegistrado(guardado, afectados(profesionalId, periodo));
    }

    @Transactional(readOnly = true)
    public List<Periodo> consultar(Long profesionalId, Clase clase, Solicitante solicitante) {
        solicitante.exigirPropioSiEsOdontologo(profesionalId);
        exigirProfesional(profesionalId);
        return registro.periodos(profesionalId, clase);
    }

    public void borrar(Long profesionalId, Clase clase, Long periodoId, Solicitante solicitante) {
        solicitante.exigirPropioSiEsOdontologo(profesionalId);
        exigirProfesional(profesionalId);
        Periodo periodo = registro.periodo(profesionalId, clase, periodoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        clase == Clase.AUSENCIA ? "la ausencia" : "el bloqueo"));
        registro.borrar(clase, periodo.id());
    }

    /** Turnos activos del profesional que se cruzan con el período, en orden de inicio. */
    private List<Turno> afectados(Long profesionalId, PeriodoSinAtencion periodo) {
        return turnos.buscarQueIntersecan(profesionalId, null, new Intervalo(periodo.inicio(), periodo.fin()))
                .stream()
                .filter(Turno::esActivo)
                .filter(t -> t.profesionalId().equals(profesionalId) && periodo.seCruzaCon(t.intervalo()))
                .sorted(Comparator.comparing(t -> t.intervalo().inicio()))
                .toList();
    }

    private void exigirProfesional(Long profesionalId) {
        if (!registro.existeProfesional(profesionalId)) {
            throw new RecursoNoEncontradoException("el profesional");
        }
    }

    /** El período guardado y los turnos que quedaron afectados. La API no expone datos del paciente. */
    public record PeriodoRegistrado(Periodo periodo, List<Turno> turnosAfectados) {}
}
