package ar.turnos.odontologia.configuracion.application;

import ar.turnos.odontologia.configuracion.application.Vistas.PrestacionVista;
import ar.turnos.odontologia.configuracion.domain.CodigoError;
import ar.turnos.odontologia.configuracion.domain.ConfiguracionException;
import ar.turnos.odontologia.configuracion.domain.DuracionPrestacion;
import ar.turnos.odontologia.configuracion.infrastructure.PrestacionRepository;
import ar.turnos.odontologia.shared.persistence.PrestacionEntity;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ABM de prestaciones (US-003). Cambiar la duración no modifica los turnos ya creados,
 * porque cada turno guarda su propio fin (design D6).
 */
@Service
@Transactional
public class PrestacionService {

    private final PrestacionRepository prestaciones;

    public PrestacionService(PrestacionRepository prestaciones) {
        this.prestaciones = prestaciones;
    }

    @Transactional(readOnly = true)
    public List<PrestacionVista> listar() {
        return prestaciones.findAllByOrderByNombreAsc().stream().map(PrestacionVista::de).toList();
    }

    @Transactional(readOnly = true)
    public PrestacionVista obtener(Long id) {
        return PrestacionVista.de(buscar(id));
    }

    public PrestacionVista crear(String nombre, Integer duracionMinutos) {
        DuracionPrestacion duracion = DuracionPrestacion.deMinutos(duracionMinutos);
        if (prestaciones.existsByNombre(nombre)) {
            throw nombreDuplicado(nombre);
        }
        return PrestacionVista.de(prestaciones.save(new PrestacionEntity(nombre, duracion.minutos())));
    }

    public PrestacionVista actualizar(Long id, String nombre, Integer duracionMinutos) {
        PrestacionEntity prestacion = buscar(id);
        DuracionPrestacion duracion = DuracionPrestacion.deMinutos(duracionMinutos);
        if (prestaciones.existsByNombreAndIdNot(nombre, id)) {
            throw nombreDuplicado(nombre);
        }
        prestacion.actualizar(nombre, duracion.minutos());
        return PrestacionVista.de(prestacion);
    }

    public PrestacionVista activar(Long id) {
        PrestacionEntity prestacion = buscar(id);
        prestacion.activar();
        return PrestacionVista.de(prestacion);
    }

    public PrestacionVista desactivar(Long id) {
        PrestacionEntity prestacion = buscar(id);
        prestacion.desactivar();
        return PrestacionVista.de(prestacion);
    }

    private PrestacionEntity buscar(Long id) {
        return prestaciones.findById(id).orElseThrow(() -> ConfiguracionException.noEncontrado("la prestación", id));
    }

    private static ConfiguracionException nombreDuplicado(String nombre) {
        return new ConfiguracionException(CodigoError.NOMBRE_DUPLICADO, "Ya existe una prestación llamada " + nombre);
    }
}
