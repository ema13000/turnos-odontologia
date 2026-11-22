package ar.turnos.odontologia.notificaciones.application;

import ar.turnos.odontologia.notificaciones.domain.Notificacion;
import ar.turnos.odontologia.notificaciones.domain.TurnoARecordar;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Puerto de salida: almacenamiento de notificaciones. Cada operación es atómica por sí sola. */
public interface NotificacionRepository {

    boolean existeRecordatorio(Long turnoId);

    /**
     * Inserta el recordatorio {@code PENDIENTE} si el turno todavía no tiene uno. Devuelve vacío si otra corrida
     * lo insertó antes: la unicidad la decide la base (design D5), no una lectura previa.
     */
    Optional<Notificacion> insertarRecordatorio(Notificacion notificacion);

    /** Inserta una notificación que no tiene límite por turno (por ejemplo un aviso de reasignación). */
    Notificacion insertar(Notificacion notificacion);

    /** Guarda el nuevo estado, canal, intentos y huella de token de una notificación ya insertada. */
    void actualizar(Notificacion notificacion);

    /**
     * Notificaciones para reprocesar, junto con los datos de su turno: las FALLIDA cuyo reintento ya venció y las
     * PENDIENTE que llevan más de 10 minutos sin enviarse. Es solo una vista: antes de enviar hay que reclamarla.
     */
    List<Reintento> reintentosVencidos(Instant ahora);

    /**
     * Toma la notificación para enviarla, en un solo paso atómico (un UPDATE condicional): solo una de varias
     * corridas simultáneas recibe {@code true}. La deja PENDIENTE con {@code programada_para = ahora}.
     */
    boolean reclamar(Notificacion vista, Instant ahora);

    Optional<Notificacion> buscarPorHuella(String huella);

    /**
     * Marca el token como usado si todavía no lo estaba, en un solo paso atómico. Devuelve {@code false} si no
     * existe o ya estaba usado.
     */
    boolean consumirToken(String huella, Instant ahora);

    record Reintento(Notificacion notificacion, TurnoARecordar turno) {}
}
