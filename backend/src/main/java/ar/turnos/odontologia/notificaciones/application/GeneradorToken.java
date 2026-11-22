package ar.turnos.odontologia.notificaciones.application;

/** Puerto de salida: genera el token de confirmación, aleatorio e imposible de adivinar (design D4). */
public interface GeneradorToken {

    String generar();
}
