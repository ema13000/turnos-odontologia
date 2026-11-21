package ar.turnos.odontologia.configuracion.application;

import ar.turnos.odontologia.configuracion.application.Vistas.SillonVista;
import ar.turnos.odontologia.configuracion.domain.CodigoError;
import ar.turnos.odontologia.configuracion.domain.ConfiguracionException;
import ar.turnos.odontologia.configuracion.infrastructure.SillonRepository;
import ar.turnos.odontologia.shared.persistence.SillonEntity;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ABM de sillones (US-002). */
@Service
@Transactional
public class SillonService {

    private final SillonRepository sillones;

    public SillonService(SillonRepository sillones) {
        this.sillones = sillones;
    }

    @Transactional(readOnly = true)
    public List<SillonVista> listar() {
        return sillones.findAllByOrderByNombreAsc().stream().map(SillonVista::de).toList();
    }

    @Transactional(readOnly = true)
    public SillonVista obtener(Long id) {
        return SillonVista.de(buscar(id));
    }

    public SillonVista crear(String nombre) {
        if (sillones.existsByNombre(nombre)) {
            throw nombreDuplicado(nombre);
        }
        return SillonVista.de(sillones.save(new SillonEntity(nombre)));
    }

    public SillonVista actualizar(Long id, String nombre) {
        SillonEntity sillon = buscar(id);
        if (sillones.existsByNombreAndIdNot(nombre, id)) {
            throw nombreDuplicado(nombre);
        }
        sillon.renombrar(nombre);
        return SillonVista.de(sillon);
    }

    public SillonVista activar(Long id) {
        SillonEntity sillon = buscar(id);
        sillon.activar();
        return SillonVista.de(sillon);
    }

    public SillonVista desactivar(Long id) {
        SillonEntity sillon = buscar(id);
        sillon.desactivar();
        return SillonVista.de(sillon);
    }

    private SillonEntity buscar(Long id) {
        return sillones.findById(id).orElseThrow(() -> ConfiguracionException.noEncontrado("el sillón", id));
    }

    private static ConfiguracionException nombreDuplicado(String nombre) {
        return new ConfiguracionException(CodigoError.NOMBRE_DUPLICADO, "Ya existe un sillón llamado " + nombre);
    }
}
