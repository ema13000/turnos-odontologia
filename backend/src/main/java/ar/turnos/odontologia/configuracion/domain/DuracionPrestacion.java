package ar.turnos.odontologia.configuracion.domain;

/**
 * Duración de una prestación en minutos: mayor que 0 y múltiplo de 5 (US-003, SU-06).
 */
public record DuracionPrestacion(int minutos) {

    private static final int MULTIPLO = 5;

    public DuracionPrestacion {
        if (minutos <= 0 || minutos % MULTIPLO != 0) {
            throw new ConfiguracionException(CodigoError.DURACION_INVALIDA,
                    "La duración debe ser mayor que 0 y múltiplo de " + MULTIPLO + " minutos");
        }
    }

    public static DuracionPrestacion deMinutos(Integer minutos) {
        if (minutos == null) {
            throw new ConfiguracionException(CodigoError.DURACION_INVALIDA, "La duración es obligatoria");
        }
        return new DuracionPrestacion(minutos);
    }
}
