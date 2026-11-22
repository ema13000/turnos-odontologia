package ar.turnos.odontologia.agenda.disponibilidad.application;

/**
 * Puerto de salida (C-07, design D10): pide el aviso de reasignación al paciente del turno nuevo (RN-AG-16). Lo
 * implementa el módulo {@code notificaciones} (C-11), que decide consentimiento, canal y contenido. {@code agenda}
 * no importa nada de {@code notificaciones}.
 *
 * <p>Se llama después de confirmar la reasignación. Si el adaptador lanza una excepción, se registra y se descarta:
 * la reasignación queda hecha igual.
 */
public interface AvisoReasignacion {

    void avisar(Long turnoId);
}
