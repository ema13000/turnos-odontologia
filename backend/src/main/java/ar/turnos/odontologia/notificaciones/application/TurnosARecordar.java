package ar.turnos.odontologia.notificaciones.application;

import ar.turnos.odontologia.notificaciones.domain.TurnoARecordar;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: lectura de turnos para armar mensajes (design D2). Devuelve registros de lectura, no
 * entidades, con lo estrictamente necesario para el mensaje.
 */
public interface TurnosARecordar {

    /**
     * Turnos activos que pueden necesitar un recordatorio: empiezan dentro de las próximas 24 horas, el paciente
     * dio su consentimiento y todavía no tienen recordatorio. El caso de uso vuelve a aplicar las reglas.
     */
    List<TurnoARecordar> candidatos(Instant ahora);

    /** Un turno, en cualquier estado, para avisar un cambio o reintentar un envío. */
    Optional<TurnoARecordar> deTurno(Long turnoId);
}
