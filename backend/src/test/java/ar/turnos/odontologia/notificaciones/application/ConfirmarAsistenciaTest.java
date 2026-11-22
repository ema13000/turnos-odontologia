package ar.turnos.odontologia.notificaciones.application;

import static ar.turnos.odontologia.notificaciones.application.NotificacionesEnMemoria.ahora;
import static ar.turnos.odontologia.notificaciones.application.NotificacionesEnMemoria.en;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.turnos.odontologia.notificaciones.application.NotificacionesEnMemoria.RelojMutable;
import ar.turnos.odontologia.notificaciones.application.NotificacionesEnMemoria.RepositorioEnMemoria;
import ar.turnos.odontologia.notificaciones.application.NotificacionesEnMemoria.TurnosEnMemoria;
import ar.turnos.odontologia.notificaciones.domain.Canal;
import ar.turnos.odontologia.notificaciones.domain.CodigoConfirmacion;
import ar.turnos.odontologia.notificaciones.domain.ConfirmacionRechazadaException;
import ar.turnos.odontologia.notificaciones.domain.Notificacion;
import ar.turnos.odontologia.notificaciones.domain.TipoNotificacion;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Casos de uso de los escenarios de {@code confirmacion-asistencia} (C-11): la confirmación por token de un solo
 * uso, con dobles en memoria. El turno es de "Paciente Uno" con la Dra. A el 2026-12-02 a las 07:30.
 */
class ConfirmarAsistenciaTest {

    private static final String TOKEN = "token-ficticio-del-recordatorio";
    private static final Instant TURNO = en(2026, 12, 2, 7, 30);

    private final TurnosEnMemoria turnos = new TurnosEnMemoria();
    private final RepositorioEnMemoria repositorio = new RepositorioEnMemoria(turnos);
    private final AgendaDeConfirmaciones agenda = new AgendaDeConfirmaciones();
    private final ConfirmarAsistencia casoDeUso =
            new ConfirmarAsistencia(repositorio, agenda, new RelojMutable(ahora()));

    /** Doble del puerto hacia la agenda: guarda el estado de cada turno como texto y confirma solo los RESERVADO. */
    static final class AgendaDeConfirmaciones implements ConfirmadorDeTurnos {
        final Map<Long, String> estados = new HashMap<>();
        int confirmaciones;
        Consumer<Long> antesDeConfirmar = id -> { };

        @Override
        public TurnoConfirmado confirmar(Long turnoId) {
            antesDeConfirmar.accept(turnoId);
            if (!"RESERVADO".equals(estados.get(turnoId))) {
                throw new ConfirmacionRechazadaException(CodigoConfirmacion.TURNO_NO_CONFIRMABLE);
            }
            estados.put(turnoId, "CONFIRMADO");
            confirmaciones++;
            return new TurnoConfirmado(TURNO, "Dra. A");
        }
    }

    private Notificacion recordatorioConToken(long turnoId, String estadoDelTurno) {
        agenda.estados.put(turnoId, estadoDelTurno);
        Notificacion pendiente = Notificacion.pendiente(turnoId, Canal.WHATSAPP, TipoNotificacion.RECORDATORIO, ahora(),
                HuellaToken.de(TOKEN));
        Notificacion guardada = repositorio.insertar(pendiente);
        repositorio.actualizar(guardada.enviada(ahora()));
        return guardada;
    }

    private Notificacion notificacion() {
        return repositorio.unica();
    }

    // ---------- Requirement: Confirmar la asistencia con el token del recordatorio ----------

    @Test
    @DisplayName("Confirmación con un token válido")
    void confirmacionConUnTokenValido() {
        // GIVEN un turno RESERVADO de "Paciente Uno" con un recordatorio enviado y su token sin usar
        recordatorioConToken(1L, "RESERVADO");

        // WHEN se presenta el token
        TurnoConfirmado confirmado = casoDeUso.ejecutar(TOKEN);

        // THEN el turno queda CONFIRMADO y la respuesta trae solo fecha, hora y profesional
        assertThat(agenda.estados.get(1L)).isEqualTo("CONFIRMADO");
        assertThat(confirmado).isEqualTo(new TurnoConfirmado(TURNO, "Dra. A"));
        // AND el token queda usado
        assertThat(notificacion().usadoEn()).isEqualTo(ahora());
    }

    // ---------- Requirement: El token es de un solo uso ----------

    @Test
    @DisplayName("Segundo uso del mismo token")
    void segundoUsoDelMismoToken() {
        // GIVEN un token que ya confirmó el turno
        recordatorioConToken(1L, "RESERVADO");
        casoDeUso.ejecutar(TOKEN);

        // WHEN se presenta otra vez
        // THEN se rechaza con TOKEN_USADO
        assertRechazo(() -> casoDeUso.ejecutar(TOKEN), CodigoConfirmacion.TOKEN_USADO);
        // AND el turno sigue CONFIRMADO y no se confirmó una segunda vez
        assertThat(agenda.estados.get(1L)).isEqualTo("CONFIRMADO");
        assertThat(agenda.confirmaciones).isEqualTo(1);
    }

    // ---------- Requirement: Tokens inexistentes y turnos que ya no se pueden confirmar ----------

    @Test
    @DisplayName("Token que no existe")
    void tokenQueNoExiste() {
        // GIVEN un turno RESERVADO con un recordatorio
        recordatorioConToken(1L, "RESERVADO");

        // WHEN se presenta un token que nunca se emitió
        // THEN se rechaza con TOKEN_INEXISTENTE
        assertRechazo(() -> casoDeUso.ejecutar("token-que-nunca-se-emitio"), CodigoConfirmacion.TOKEN_INEXISTENTE);
        // AND no cambia ningún turno
        assertThat(agenda.estados.get(1L)).isEqualTo("RESERVADO");
        assertThat(agenda.confirmaciones).isZero();
    }

    @Test
    @DisplayName("Turno cancelado después del recordatorio")
    void turnoCanceladoDespuesDelRecordatorio() {
        // GIVEN un turno de "Paciente Uno" que se canceló después de enviarse el recordatorio, con su token sin usar
        recordatorioConToken(1L, "CANCELADO");

        // WHEN se intenta confirmar con ese token
        // THEN se rechaza con TURNO_NO_CONFIRMABLE
        assertRechazo(() -> casoDeUso.ejecutar(TOKEN), CodigoConfirmacion.TURNO_NO_CONFIRMABLE);
        // AND el turno sigue CANCELADO y el token no se consumió
        assertThat(agenda.estados.get(1L)).isEqualTo("CANCELADO");
        assertThat(notificacion().usadoEn()).isNull();
    }

    @Test
    @DisplayName("Turno ya confirmado")
    void turnoYaConfirmado() {
        // GIVEN un turno que ya está CONFIRMADO, con un token sin usar
        recordatorioConToken(1L, "CONFIRMADO");

        // WHEN se intenta confirmar con ese token
        // THEN se rechaza con TURNO_NO_CONFIRMABLE
        assertRechazo(() -> casoDeUso.ejecutar(TOKEN), CodigoConfirmacion.TURNO_NO_CONFIRMABLE);
        assertThat(notificacion().usadoEn()).isNull();
    }

    @Test
    void siElTokenSeUsaEnParaleloGanaUnoSoloYElOtroRecibeTokenUsado() {
        // otra confirmación gana justo mientras esta confirma: el turno ya no está RESERVADO y el token ya está usado
        recordatorioConToken(1L, "RESERVADO");
        agenda.antesDeConfirmar = id -> {
            repositorio.consumirToken(HuellaToken.de(TOKEN), ahora());
            agenda.estados.put(id, "CONFIRMADO");
        };

        assertRechazo(() -> casoDeUso.ejecutar(TOKEN), CodigoConfirmacion.TOKEN_USADO);
    }

    @Test
    void elTokenSeGuardaSoloComoHuellaYNuncaEnClaro() {
        recordatorioConToken(1L, "RESERVADO");

        assertThat(notificacion().tokenConfirmacion()).isEqualTo(HuellaToken.de(TOKEN)).isNotEqualTo(TOKEN)
                .hasSize(64);
    }

    private static void assertRechazo(org.assertj.core.api.ThrowableAssert.ThrowingCallable accion,
                                      CodigoConfirmacion esperado) {
        assertThatThrownBy(accion)
                .isInstanceOf(ConfirmacionRechazadaException.class)
                .extracting(e -> ((ConfirmacionRechazadaException) e).codigo())
                .isEqualTo(esperado);
    }
}
