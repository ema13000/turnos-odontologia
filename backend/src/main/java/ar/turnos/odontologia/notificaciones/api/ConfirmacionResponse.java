package ar.turnos.odontologia.notificaciones.api;

/**
 * Respuesta de la confirmación: solo lo necesario para que el paciente vea qué confirmó. Sin DNI, email, teléfono,
 * prestación ni datos de otros pacientes.
 *
 * @param fecha {@code yyyy-MM-dd} en la zona de Buenos Aires
 * @param hora {@code HH:mm} en la zona de Buenos Aires
 * @param estado siempre {@code CONFIRMADO}
 */
public record ConfirmacionResponse(String fecha, String hora, String profesional, String estado) {}
