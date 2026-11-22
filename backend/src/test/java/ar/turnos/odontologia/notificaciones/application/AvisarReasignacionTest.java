package ar.turnos.odontologia.notificaciones.application;

import static ar.turnos.odontologia.notificaciones.application.NotificacionesEnMemoria.ahora;
import static ar.turnos.odontologia.notificaciones.application.NotificacionesEnMemoria.en;
import static ar.turnos.odontologia.notificaciones.application.NotificacionesEnMemoria.soloEmail;
import static org.assertj.core.api.Assertions.assertThat;

import ar.turnos.odontologia.notificaciones.application.NotificacionesEnMemoria.CanalEnMemoria;
import ar.turnos.odontologia.notificaciones.application.NotificacionesEnMemoria.RelojMutable;
import ar.turnos.odontologia.notificaciones.application.NotificacionesEnMemoria.RepositorioEnMemoria;
import ar.turnos.odontologia.notificaciones.application.NotificacionesEnMemoria.TurnosEnMemoria;
import ar.turnos.odontologia.notificaciones.domain.Canal;
import ar.turnos.odontologia.notificaciones.domain.Consultorio;
import ar.turnos.odontologia.notificaciones.domain.ContactoPaciente;
import ar.turnos.odontologia.notificaciones.domain.EstadoNotificacion;
import ar.turnos.odontologia.notificaciones.domain.Notificacion;
import ar.turnos.odontologia.notificaciones.domain.TipoNotificacion;
import ar.turnos.odontologia.notificaciones.domain.TurnoARecordar;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Escenarios de "Aviso de reasignación por ausencia" del spec {@code notificaciones-recordatorios} (C-11, RN-AG-16),
 * con dobles en memoria. El turno de "Paciente Uno" se reasignó al 2026-12-03 a las 10:00 con el Dr. B.
 */
class AvisarReasignacionTest {

    private static final Instant NUEVO_HORARIO = en(2026, 12, 3, 10, 0);

    private final TurnosEnMemoria turnos = new TurnosEnMemoria();
    private final RepositorioEnMemoria repositorio = new RepositorioEnMemoria(turnos);
    private final CanalEnMemoria whatsapp = new CanalEnMemoria(Canal.WHATSAPP);
    private final CanalEnMemoria email = new CanalEnMemoria(Canal.EMAIL);
    private final RelojMutable reloj = new RelojMutable(ahora());
    private final Consultorio consultorio = new Consultorio("Clínica Ejemplo", "Calle Ficticia 123");
    private final AvisarReasignacion casoDeUso = new AvisarReasignacion(turnos, repositorio, List.of(whatsapp, email),
            consultorio, reloj);

    private void reasignadoA(ContactoPaciente contacto) {
        turnos.guardar(new TurnoARecordar(1L, NUEVO_HORARIO, en(2026, 11, 28, 12, 0), true,
                contacto));
    }

    @Test
    @DisplayName("Aviso con consentimiento")
    void avisoConConsentimiento() {
        // GIVEN un turno de "Paciente Uno" reasignado al 2026-12-03 a las 10:00 con otro profesional
        // AND "Paciente Uno" con consentimiento y email
        reasignadoA(soloEmail());

        // WHEN se pide avisarle la reasignación
        casoDeUso.ejecutar(1L);

        // THEN se envía un aviso por email con la nueva fecha, la nueva hora y el nuevo profesional, sin datos de salud
        assertThat(email.enviados).hasSize(1);
        assertThat(email.enviados.get(0).cuerpo()).contains("03/12/2026", "10:00").doesNotContain("Dr. B", "Paciente Uno")
                .doesNotContain("Consulta").doesNotContain("http");
        Notificacion aviso = repositorio.unica();
        assertThat(aviso.tipo()).isEqualTo(TipoNotificacion.REASIGNACION);
        assertThat(aviso.estado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(aviso.tokenConfirmacion()).isNull();
    }

    @Test
    @DisplayName("Aviso sin consentimiento")
    void avisoSinConsentimiento() {
        // GIVEN el mismo turno reasignado y "Paciente Uno" sin consentimiento
        reasignadoA(new ContactoPaciente("paciente.uno@example.com", "+54 11 5555-0001", true, false));

        // WHEN se pide avisarle la reasignación
        casoDeUso.ejecutar(1L);

        // THEN no se envía ningún mensaje
        assertThat(email.enviados).isEmpty();
        assertThat(whatsapp.enviados).isEmpty();
        assertThat(repositorio.notificaciones).isEmpty();
    }

    @Test
    void dosReasignacionesDelMismoTurnoGeneranDosAvisos() {
        reasignadoA(soloEmail());

        casoDeUso.ejecutar(1L);
        casoDeUso.ejecutar(1L);

        assertThat(email.enviados).hasSize(2);
        assertThat(repositorio.deTipo(TipoNotificacion.REASIGNACION)).hasSize(2);
    }

    @Test
    void unTurnoInexistenteNoGeneraNada() {
        casoDeUso.ejecutar(99L);

        assertThat(repositorio.notificaciones).isEmpty();
    }

    @Test
    void siElEnvioFallaElAvisoQuedaFallidoConReintento() {
        reasignadoA(soloEmail());
        email.falla = true;

        casoDeUso.ejecutar(1L);

        Notificacion aviso = repositorio.unica();
        assertThat(aviso.estado()).isEqualTo(EstadoNotificacion.FALLIDA);
        assertThat(aviso.proximoIntento()).isNotNull();
    }

    @Test
    void laReasignacionNuncaGuardaHashDeTokenNiAlEnviarseNiAlReintentarse() {
        reasignadoA(soloEmail());
        email.falla = true;
        casoDeUso.ejecutar(1L);
        assertThat(repositorio.unica().tokenConfirmacion()).isNull();

        // el reintento lo hace la tarea de recordatorios: la reasignación sigue sin token
        email.falla = false;
        reloj.adelantarMinutos(5);
        new EnviarRecordatorios(turnos, repositorio, List.of(whatsapp, email),
                new NotificacionesEnMemoria.TokensSecuenciales(), consultorio, "https://turnos.example.test", reloj)
                .ejecutar();

        Notificacion aviso = repositorio.unica();
        assertThat(aviso.estado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(aviso.tokenConfirmacion()).isNull();
        assertThat(email.enviados.get(0).cuerpo()).doesNotContain("http");
    }
}
