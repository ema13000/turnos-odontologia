package ar.turnos.odontologia.configuracion.api;

import ar.turnos.odontologia.configuracion.api.Solicitudes.SillonSolicitud;
import ar.turnos.odontologia.configuracion.application.SillonService;
import ar.turnos.odontologia.configuracion.application.Vistas.SillonVista;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Sillones (US-002). Sin {@code DELETE}: se desactiva (RN-AG-11). */
@RestController
@RequestMapping("/api/v1/sillones")
public class SillonController {

    private final SillonService servicio;

    public SillonController(SillonService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<SillonVista> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public SillonVista obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    public ResponseEntity<SillonVista> crear(@Valid @RequestBody SillonSolicitud solicitud) {
        SillonVista creado = servicio.crear(solicitud.nombre());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{id}")
    public SillonVista actualizar(@PathVariable Long id, @Valid @RequestBody SillonSolicitud solicitud) {
        return servicio.actualizar(id, solicitud.nombre());
    }

    @PostMapping("/{id}/activar")
    public SillonVista activar(@PathVariable Long id) {
        return servicio.activar(id);
    }

    @PostMapping("/{id}/desactivar")
    public SillonVista desactivar(@PathVariable Long id) {
        return servicio.desactivar(id);
    }
}
