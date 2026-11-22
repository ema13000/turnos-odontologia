package ar.turnos.odontologia.notificaciones.application;

import ar.turnos.odontologia.notificaciones.domain.Canal;
import ar.turnos.odontologia.notificaciones.domain.ContactoPaciente;
import ar.turnos.odontologia.notificaciones.domain.Notificacion;
import ar.turnos.odontologia.notificaciones.domain.TipoNotificacion;
import ar.turnos.odontologia.notificaciones.domain.TurnoARecordar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dobles de prueba de los puertos de notificaciones (datos ficticios): turnos a recordar, repositorio, canales y
 * generador de tokens, más un reloj que se puede adelantar.
 */
final class NotificacionesEnMemoria {

    static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");

    private NotificacionesEnMemoria() {}

    static Instant en(int anio, int mes, int dia, int hora, int minuto) {
        return LocalDateTime.of(anio, mes, dia, hora, minuto).atZone(BUENOS_AIRES).toInstant();
    }

    /** "Ahora" de los escenarios: 2026-12-01 a las 08:00. */
    static Instant ahora() {
        return en(2026, 12, 1, 8, 0);
    }

    static ContactoPaciente conWhatsappYEmail() {
        return new ContactoPaciente("paciente.uno@example.com", "+54 11 5555-0001", true, true);
    }

    static ContactoPaciente soloWhatsapp() {
        return new ContactoPaciente(null, "+54 11 5555-0001", true, true);
    }

    static ContactoPaciente soloEmail() {
        return new ContactoPaciente("paciente.uno@example.com", null, false, true);
    }

    /** Turno activo, reservado el 2026-11-28. */
    static TurnoARecordar turno(long id, Instant inicio, ContactoPaciente contacto) {
        return new TurnoARecordar(id, inicio, en(2026, 11, 28, 12, 0), true, contacto);
    }

    static final class RelojMutable extends Clock {
        private Instant instante;

        RelojMutable(Instant instante) {
            this.instante = instante;
        }

        void adelantarMinutos(long minutos) {
            instante = instante.plusSeconds(minutos * 60);
        }

        void en(Instant nuevo) {
            instante = nuevo;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }

    static final class TurnosEnMemoria implements TurnosARecordar {
        final Map<Long, TurnoARecordar> turnos = new HashMap<>();

        void guardar(TurnoARecordar turno) {
            turnos.put(turno.turnoId(), turno);
        }

        @Override
        public List<TurnoARecordar> candidatos(Instant ahora) {
            return new ArrayList<>(turnos.values());
        }

        @Override
        public Optional<TurnoARecordar> deTurno(Long turnoId) {
            return Optional.ofNullable(turnos.get(turnoId));
        }
    }

    static class RepositorioEnMemoria implements NotificacionRepository {
        final List<Notificacion> notificaciones = new ArrayList<>();
        private final TurnosEnMemoria turnos;
        private long proximoId = 1;

        RepositorioEnMemoria(TurnosEnMemoria turnos) {
            this.turnos = turnos;
        }

        List<Notificacion> deTipo(TipoNotificacion tipo) {
            return notificaciones.stream().filter(n -> n.tipo() == tipo).toList();
        }

        Notificacion unica() {
            if (notificaciones.size() != 1) {
                throw new AssertionError("Se esperaba una notificación y hay " + notificaciones.size());
            }
            return notificaciones.get(0);
        }

        @Override
        public synchronized boolean existeRecordatorio(Long turnoId) {
            return notificaciones.stream()
                    .anyMatch(n -> n.tipo() == TipoNotificacion.RECORDATORIO && n.turnoId().equals(turnoId));
        }

        @Override
        public synchronized Optional<Notificacion> insertarRecordatorio(Notificacion notificacion) {
            boolean yaExiste = notificaciones.stream()
                    .anyMatch(n -> n.tipo() == TipoNotificacion.RECORDATORIO && n.turnoId().equals(notificacion.turnoId()));
            if (yaExiste) {
                return Optional.empty();
            }
            return Optional.of(insertar(notificacion));
        }

        @Override
        public synchronized Notificacion insertar(Notificacion notificacion) {
            Notificacion guardada = notificacion.conId(proximoId++);
            notificaciones.add(guardada);
            return guardada;
        }

        @Override
        public synchronized void actualizar(Notificacion notificacion) {
            notificaciones.replaceAll(n -> n.id().equals(notificacion.id()) ? notificacion : n);
        }

        @Override
        public synchronized boolean reclamar(Notificacion vista, Instant ahora) {
            // atómico como el UPDATE condicional: solo gana quien la encuentra todavía vencida
            for (int i = 0; i < notificaciones.size(); i++) {
                Notificacion actual = notificaciones.get(i);
                if (actual.id().equals(vista.id())) {
                    if (!actual.reintentoVencido(ahora)) {
                        return false;
                    }
                    notificaciones.set(i, actual.reclamada(ahora));
                    return true;
                }
            }
            return false;
        }

        @Override
        public synchronized List<Reintento> reintentosVencidos(Instant ahora) {
            return notificaciones.stream()
                    .filter(n -> n.reintentoVencido(ahora))
                    .map(n -> new Reintento(n, turnos.deTurno(n.turnoId()).orElseThrow()))
                    .toList();
        }

        @Override
        public Optional<Notificacion> buscarPorHuella(String huella) {
            return notificaciones.stream().filter(n -> huella.equals(n.tokenConfirmacion())).findFirst();
        }

        @Override
        public boolean consumirToken(String huella, Instant ahora) {
            Optional<Notificacion> encontrada = buscarPorHuella(huella);
            if (encontrada.isEmpty() || encontrada.get().usadoEn() != null) {
                return false;
            }
            Notificacion n = encontrada.get();
            notificaciones.replaceAll(x -> x.id().equals(n.id()) ? new Notificacion(n.id(), n.turnoId(), n.canal(),
                    n.tipo(), n.estado(), n.programadaPara(), n.enviadaEn(), n.tokenConfirmacion(), ahora,
                    n.intentos(), n.proximoIntento(), n.canalAlternativo()) : x);
            return true;
        }
    }

    /** Canal que anota lo que recibe y falla cuando se le pide. */
    static final class CanalEnMemoria implements NotificacionPort {
        private final Canal canal;
        boolean falla;
        final List<EnvioNotificacion> enviados = new ArrayList<>();
        int intentos;

        CanalEnMemoria(Canal canal) {
            this.canal = canal;
        }

        @Override
        public Canal canal() {
            return canal;
        }

        @Override
        public synchronized ResultadoEnvio enviar(EnvioNotificacion envio) {
            intentos++;
            if (falla) {
                return ResultadoEnvio.FALLO;
            }
            enviados.add(envio);
            return ResultadoEnvio.OK;
        }
    }

    static final class TokensSecuenciales implements GeneradorToken {
        private int n;

        @Override
        public String generar() {
            return "token-ficticio-" + (++n);
        }
    }
}
