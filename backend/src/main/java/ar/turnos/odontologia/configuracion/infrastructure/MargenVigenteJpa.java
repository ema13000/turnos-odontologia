package ar.turnos.odontologia.configuracion.infrastructure;

import ar.turnos.odontologia.agenda.domain.Margen;
import ar.turnos.odontologia.configuracion.application.MargenVigente;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lee el margen de la tabla {@code configuracion} en cada llamada, sin caché, para que un cambio
 * aplique de inmediato (design D7).
 */
@Component
public class MargenVigenteJpa implements MargenVigente {

    public static final String CLAVE = "margen_minutos";

    private final ConfiguracionRepository configuracion;

    public MargenVigenteJpa(ConfiguracionRepository configuracion) {
        this.configuracion = configuracion;
    }

    @Override
    @Transactional(readOnly = true)
    public Margen actual() {
        return configuracion.findById(CLAVE)
                .map(fila -> Margen.deMinutos(Integer.parseInt(fila.getValor())))
                .orElse(Margen.PREDETERMINADO);
    }
}
