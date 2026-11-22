package ar.turnos.odontologia.configuracion.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Cuerpos de entrada de la API de configuración. Los límites siguen las columnas de V1. */
public final class Solicitudes {

    private Solicitudes() {
    }

    public record ProfesionalSolicitud(
            @NotBlank @Size(max = 100) String nombre,
            @NotBlank @Size(max = 100) String apellido,
            @NotBlank @Size(max = 30) String matricula) {
    }

    public record SillonSolicitud(@NotBlank @Size(max = 50) String nombre) {
    }

    /** La duración se valida en el dominio para responder {@code DURACION_INVALIDA}. */
    public record PrestacionSolicitud(@NotBlank @Size(max = 100) String nombre, Integer duracionMinutos) {
    }

    public record EspecialidadesSolicitud(@NotNull List<@NotNull Long> prestacionIds) {
    }

    /** {@code sillonId} nulo quita el sillón habitual. */
    public record SillonHabitualSolicitud(Long sillonId) {
    }

    /** El mínimo se valida en el dominio para responder {@code MARGEN_INVALIDO}. */
    public record MargenDto(Integer minutos) {
    }
}
