package ar.turnos.odontologia.agenda.api;

import static org.assertj.core.api.Assertions.assertThat;

import ar.turnos.odontologia.agenda.IntegracionAgendaBase;
import ar.turnos.odontologia.shared.security.Navegador;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.web.servlet.MvcResult;

/** Carrera por el último sillón libre, de punta a punta por la API (RN-AG-06, D5). */
@Tag("integration")
class CarreraSobreturnoIntegrationTest extends IntegracionAgendaBase {

    private static final String MOTIVO = "Urgencia administrativa ficticia";

    @RepeatedTest(10)
    @DisplayName("Dos sobreturnos al mismo tiempo")
    void dosSobreturnosAlMismoTiempo() throws Exception {
        // GIVEN recepcion y ana.ejemplo con sesión iniciada
        Navegador recepcion = como("recepcion");
        Navegador ana = como("ana.ejemplo");
        // AND solo el Sillón 1 libre a las 10:00, con un turno previo que ocupa el Sillón 2 con margen
        sembrarSillon2Ocupado();
        CountDownLatch largada = new CountDownLatch(1);

        // WHEN los dos piden al mismo tiempo un sobreturno de "Consulta" con inicio 10:00
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            List<Future<MvcResult>> pedidos = new ArrayList<>();
            pedidos.add(hilos.submit(enLaLargada(largada, () -> crearSobreturno(recepcion))));
            pedidos.add(hilos.submit(enLaLargada(largada, () -> crearSobreturno(ana))));
            largada.countDown();

            List<Integer> estados = new ArrayList<>();
            List<String> codigos = new ArrayList<>();
            for (Future<MvcResult> pedido : pedidos) {
                MvcResult resultado = pedido.get();
                estados.add(resultado.getResponse().getStatus());
                if (resultado.getResponse().getStatus() == 409) {
                    codigos.add(cuerpo(resultado).get("codigo").asText());
                }
            }

            // THEN una respuesta es 201
            assertThat(estados).containsExactlyInAnyOrder(201, 409);
            // AND la otra es 409 con el código SIN_SILLON_DISPONIBLE
            assertThat(codigos).containsExactly("SIN_SILLON_DISPONIBLE");
            // AND solo hay un sobreturno activo a las 10:00
            assertThat(jdbc.queryForObject("SELECT count(*) FROM turno WHERE es_sobreturno AND estado "
                    + "IN ('RESERVADO','CONFIRMADO')", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM turno_auditoria WHERE accion = 'SOBRETURNO'",
                    Integer.class)).isEqualTo(1);
        } finally {
            hilos.shutdownNow();
        }
    }

    @RepeatedTest(10)
    @DisplayName("Dos sobreturnos al mismo tiempo con dos sillones libres")
    void dosSobreturnosAlMismoTiempoConDosSillonesLibres() throws Exception {
        // GIVEN recepcion y ana.ejemplo con sesión iniciada, con el Sillón 1 y el Sillón 2 libres a las 10:00
        Navegador recepcion = como("recepcion");
        Navegador ana = como("ana.ejemplo");
        CountDownLatch largada = new CountDownLatch(1);

        // WHEN los dos piden al mismo tiempo un sobreturno de "Consulta" con inicio 10:00
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            List<Future<MvcResult>> pedidos = new ArrayList<>();
            pedidos.add(hilos.submit(enLaLargada(largada, () -> crearSobreturno(recepcion))));
            pedidos.add(hilos.submit(enLaLargada(largada, () -> crearSobreturno(ana))));
            largada.countDown();

            List<Integer> estados = new ArrayList<>();
            for (Future<MvcResult> pedido : pedidos) {
                estados.add(pedido.get().getResponse().getStatus());
            }

            // THEN las dos respuestas son 201
            assertThat(estados).containsExactly(201, 201);
            // AND los dos sobreturnos quedan en sillones distintos
            assertThat(jdbc.queryForObject("SELECT count(DISTINCT sillon_id) FROM turno WHERE es_sobreturno",
                    Integer.class)).isEqualTo(2);
            // AND hay un registro de auditoría SOBRETURNO por cada uno
            assertThat(jdbc.queryForObject("SELECT count(*) FROM turno_auditoria WHERE accion = 'SOBRETURNO'",
                    Integer.class)).isEqualTo(2);
        } finally {
            hilos.shutdownNow();
        }
    }

    private MvcResult crearSobreturno(Navegador navegador) throws Exception {
        return navegador.post(TURNOS + "/sobreturno", json.writeValueAsString(Map.of(
                "pacienteId", pacienteUno,
                "profesionalId", draA,
                "prestacionId", consulta,
                "inicio", hora(10, 0),
                "motivo", MOTIVO))).andReturn();
    }

    private void sembrarSillon2Ocupado() {
        Instant inicio = LocalDateTime.of(2026, 12, 1, 10, 0).atZone(BUENOS_AIRES).toInstant();
        Instant fin = inicio.plusSeconds(30 * 60);
        jdbc.update("INSERT INTO turno(profesional_id, sillon_id, prestacion_id, paciente_id, inicio, fin, "
                        + "fin_con_margen, estado, es_sobreturno) VALUES (?,?,?,?,?,?,?,'RESERVADO',false)",
                drB, sillon2, consulta, pacienteDos, inicio.atOffset(ZoneOffset.UTC), fin.atOffset(ZoneOffset.UTC),
                fin.plusSeconds(10 * 60).atOffset(ZoneOffset.UTC));
    }

    private static <T> Callable<T> enLaLargada(CountDownLatch largada, Callable<T> pedido) {
        return () -> {
            largada.await();
            return pedido.call();
        };
    }
}
