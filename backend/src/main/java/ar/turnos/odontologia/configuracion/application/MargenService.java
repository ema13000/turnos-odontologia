package ar.turnos.odontologia.configuracion.application;

import ar.turnos.odontologia.agenda.domain.CodigoRechazo;
import ar.turnos.odontologia.agenda.domain.Margen;
import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;
import ar.turnos.odontologia.configuracion.domain.CodigoError;
import ar.turnos.odontologia.configuracion.domain.ConfiguracionException;
import ar.turnos.odontologia.configuracion.infrastructure.ConfiguracionEntity;
import ar.turnos.odontologia.configuracion.infrastructure.ConfiguracionRepository;
import ar.turnos.odontologia.configuracion.infrastructure.MargenVigenteJpa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta y cambio del margen entre turnos (RN-AG-04). La regla del mínimo de 10 minutos vive
 * en {@link Margen} (C-01); acá solo se persiste un valor ya validado.
 */
@Service
@Transactional
public class MargenService {

    private final MargenVigente margenVigente;
    private final ConfiguracionRepository configuracion;

    public MargenService(MargenVigente margenVigente, ConfiguracionRepository configuracion) {
        this.margenVigente = margenVigente;
        this.configuracion = configuracion;
    }

    @Transactional(readOnly = true)
    public long minutos() {
        return margenVigente.actual().valor().toMinutes();
    }

    public long cambiar(Integer minutos) {
        Margen margen = validar(minutos);
        long nuevo = margen.valor().toMinutes();
        ConfiguracionEntity fila = configuracion.findById(MargenVigenteJpa.CLAVE)
                .orElseGet(() -> new ConfiguracionEntity(MargenVigenteJpa.CLAVE, String.valueOf(nuevo)));
        fila.cambiarValor(String.valueOf(nuevo));
        configuracion.save(fila);
        return nuevo;
    }

    private static Margen validar(Integer minutos) {
        if (minutos == null) {
            throw new ConfiguracionException(CodigoError.MARGEN_INVALIDO, "El margen es obligatorio");
        }
        try {
            return Margen.deMinutos(minutos);
        } catch (TurnoRechazadoException ex) {
            if (ex.codigo() == CodigoRechazo.MARGEN_INVALIDO) {
                throw new ConfiguracionException(CodigoError.MARGEN_INVALIDO, ex.getMessage());
            }
            throw ex;
        }
    }
}
