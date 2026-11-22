package ar.turnos.odontologia.notificaciones.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Escenarios de "Mensaje sin datos de salud" del spec {@code notificaciones-recordatorios} (C-11, RN-NO-01, regla
 * dura 6). El mensaje lleva solo fecha, hora, consultorio y link: ni el nombre del paciente ni el del profesional.
 * El momento del turno es el 2026-12-02 a las 07:30 en Buenos Aires (10:30 UTC).
 */
class MensajeRecordatorioTest {

    private static final Instant SIETE_Y_MEDIA = Instant.parse("2026-12-02T10:30:00Z");
    private static final Consultorio CONSULTORIO = new Consultorio("Clínica Ejemplo", "Calle Ficticia 123");
    private static final String LINK = "https://turnos.example.test/confirmar/abc123";

    @Test
    @DisplayName("Contenido del recordatorio")
    void contenidoDelRecordatorio() {
        // GIVEN un turno de "Paciente Uno" de "Consulta" con la Dra. A el 2026-12-02 a las 07:30
        // (el dato de entrada no tiene dónde poner el paciente, el profesional ni la prestación)
        DatosDelTurno turno = new DatosDelTurno(SIETE_Y_MEDIA);

        // WHEN se arma el recordatorio
        String texto = MensajeRecordatorio.recordatorio(turno, CONSULTORIO, LINK).texto();

        // THEN el mensaje contiene la fecha, la hora, el consultorio, su dirección y el link de confirmar
        assertThat(texto).contains("02/12/2026", "07:30", "Clínica Ejemplo", "Calle Ficticia 123", LINK);
        // AND no contiene "Consulta" ni ningún otro nombre de prestación
        assertThat(texto).doesNotContain("Consulta ");
        // AND no contiene el nombre de "Paciente Uno" ni el de la Dra. A
        assertThat(texto).doesNotContain("Paciente Uno", "Dra. A");
    }

    @Test
    @DisplayName("Un sobreturno no filtra su motivo")
    void unSobreturnoNoFiltraSuMotivo() {
        // GIVEN un sobreturno de "Paciente Uno" con un motivo administrativo (el read model no lo trae)
        String motivo = "Paciente derivado por la obra social";
        TurnoARecordar sobreturno = new TurnoARecordar(7L, SIETE_Y_MEDIA, Instant.parse("2026-11-28T12:00:00Z"), true,
                new ContactoPaciente("paciente.uno@example.com", null, false, true));

        // WHEN se arma el recordatorio
        String texto = MensajeRecordatorio.recordatorio(sobreturno.datosDelTurno(), CONSULTORIO, LINK).texto();

        // THEN el mensaje no contiene el motivo
        assertThat(texto).doesNotContain(motivo);
    }

    @Test
    void ningunRegistroDeLecturaNiPlantillaTieneCamposDeNombreNiDeSalud() throws Exception {
        for (Class<? extends Record> tipo : List.of(DatosDelTurno.class, TurnoARecordar.class,
                ContactoPaciente.class)) {
            assertThat(Arrays.stream(tipo.getRecordComponents()).map(RecordComponent::getName))
                    .as("campos de %s", tipo.getSimpleName())
                    .noneMatch(c -> {
                        String minuscula = c.toLowerCase();
                        return minuscula.contains("nombre") || minuscula.contains("apellido")
                                || minuscula.contains("prestacion") || minuscula.contains("motivo")
                                || minuscula.contains("clinic") || minuscula.contains("profesional");
                    });
        }
        // las plantillas solo reciben cuatro datos (reasignación) o cinco (recordatorio): fecha, hora, consultorio,
        // dirección y, en el recordatorio, el link
        assertThat(placeholders(MensajeRecordatorio.class.getDeclaredField("PLANTILLA_RECORDATORIO"))).isEqualTo(5);
        assertThat(placeholders(MensajeRecordatorio.class.getDeclaredField("PLANTILLA_REASIGNACION"))).isEqualTo(4);
    }

    @Test
    void fechaYHoraEnLaZonaDeBuenosAires() {
        // 2026-12-03T01:30Z es el 2 de diciembre a las 22:30 en Buenos Aires (UTC-3), no el 3
        DatosDelTurno nocturno = new DatosDelTurno(Instant.parse("2026-12-03T01:30:00Z"));

        assertThat(MensajeRecordatorio.recordatorio(nocturno, CONSULTORIO, LINK).texto())
                .contains("02/12/2026", "22:30");
    }

    @Test
    void elAvisoDeReasignacionTraeLaNuevaFechaYHoraSinNombresNiLink() {
        DatosDelTurno reasignado = new DatosDelTurno(Instant.parse("2026-12-03T13:00:00Z"));

        String texto = MensajeRecordatorio.reasignacion(reasignado, CONSULTORIO).texto();

        assertThat(texto).contains("03/12/2026", "10:00", "Clínica Ejemplo", "Calle Ficticia 123")
                .doesNotContain("http", "Paciente Uno", "Dr. B");
    }

    private static int placeholders(Field plantilla) throws IllegalAccessException {
        assertThat(Modifier.isStatic(plantilla.getModifiers())).isTrue();
        plantilla.setAccessible(true);
        String texto = (String) plantilla.get(null);
        return texto.split("%s", -1).length - 1;
    }
}
