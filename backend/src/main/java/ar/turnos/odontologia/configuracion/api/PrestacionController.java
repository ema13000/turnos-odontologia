package ar.turnos.odontologia.configuracion.api;

import ar.turnos.odontologia.configuracion.api.Solicitudes.PrestacionSolicitud;
import ar.turnos.odontologia.configuracion.application.PrestacionService;
import ar.turnos.odontologia.configuracion.application.Vistas.PrestacionVista;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Prestaciones con su duración (US-003). Sin {@code DELETE}: se desactiva (RN-AG-11). */
@RestController
@RequestMapping("/api/v1/prestaciones")
public class PrestacionController {

    /** Solo ADMIN modifica la configuración; el resto del personal consulta (matriz RBAC de la KB). */
    private static final String SOLO_ADMIN = "hasRole('ADMIN')";

    private final PrestacionService servicio;

    public PrestacionController(PrestacionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<PrestacionVista> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public PrestacionVista obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PreAuthorize(SOLO_ADMIN)
    @PostMapping
    public ResponseEntity<PrestacionVista> crear(@Valid @RequestBody PrestacionSolicitud solicitud) {
        PrestacionVista creada = servicio.crear(solicitud.nombre(), solicitud.duracionMinutos());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @PreAuthorize(SOLO_ADMIN)
    @PutMapping("/{id}")
    public PrestacionVista actualizar(@PathVariable Long id, @Valid @RequestBody PrestacionSolicitud solicitud) {
        return servicio.actualizar(id, solicitud.nombre(), solicitud.duracionMinutos());
    }

    @PreAuthorize(SOLO_ADMIN)
    @PostMapping("/{id}/activar")
    public PrestacionVista activar(@PathVariable Long id) {
        return servicio.activar(id);
    }

    @PreAuthorize(SOLO_ADMIN)
    @PostMapping("/{id}/desactivar")
    public PrestacionVista desactivar(@PathVariable Long id) {
        return servicio.desactivar(id);
    }
}
