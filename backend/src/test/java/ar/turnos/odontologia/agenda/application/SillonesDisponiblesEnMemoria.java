package ar.turnos.odontologia.agenda.application;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Doble de prueba de {@link SillonesDisponibles}: devuelve los sillones activos ordenados por id. */
class SillonesDisponiblesEnMemoria implements SillonesDisponibles {

    private final RecursosAgendaEnMemoria recursos;
    private final List<Long> ids;
    private Long preferido;
    private int consultasPreferido;

    SillonesDisponiblesEnMemoria(RecursosAgendaEnMemoria recursos, Long... ids) {
        this.recursos = recursos;
        this.ids = Arrays.stream(ids).sorted().toList();
    }

    @Override
    public List<Long> activos() {
        return ids.stream().filter(id -> recursos.sillonActivo(id).orElse(false)).toList();
    }

    void preferir(Long id) {
        this.preferido = id;
    }

    int consultasPreferido() {
        return consultasPreferido;
    }

    @Override
    public Optional<Long> preferidoPara(Long profesionalId) {
        consultasPreferido++;
        return Optional.ofNullable(preferido);
    }
}
