package ar.turnos.odontologia.notificaciones.application;

import java.time.Instant;

/** Lo único que se le muestra al paciente al confirmar: cuándo es el turno y con quién. */
public record TurnoConfirmado(Instant inicio, String nombreProfesional) {}
