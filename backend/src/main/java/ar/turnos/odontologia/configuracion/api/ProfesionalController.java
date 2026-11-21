package ar.turnos.odontologia.configuracion.api;

import ar.turnos.odontologia.configuracion.api.Solicitudes.EspecialidadesSolicitud;
import ar.turnos.odontologia.configuracion.api.Solicitudes.ProfesionalSolicitud;
import ar.turnos.odontologia.configuracion.api.Solicitudes.SillonHabitualSolicitud;
import ar.turnos.odontologia.configuracion.application.ProfesionalService;
import ar.turnos.odontologia.configuracion.application.Vistas.PrestacionVista;
import ar.turnos.odontologia.configuracion.application.Vistas.ProfesionalVista;
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

/**
 * Profesionales, especialidades y sillón habitual (US-001, US-018). Sin {@code DELETE}: se
 * desactiva (RN-AG-11). Sin autenticación hasta C-05, que debe restringirlo al rol ADMIN.
 */
@RestController
@RequestMapping("/api/v1/profesionales")
public class ProfesionalController {

    private final ProfesionalService servicio;

    public ProfesionalController(ProfesionalService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<ProfesionalVista> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public ProfesionalVista obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    public ResponseEntity<ProfesionalVista> crear(@Valid @RequestBody ProfesionalSolicitud solicitud) {
        ProfesionalVista creado = servicio.crear(solicitud.nombre(), solicitud.apellido(), solicitud.matricula());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{id}")
    public ProfesionalVista actualizar(@PathVariable Long id, @Valid @RequestBody ProfesionalSolicitud solicitud) {
        return servicio.actualizar(id, solicitud.nombre(), solicitud.apellido(), solicitud.matricula());
    }

    @PostMapping("/{id}/activar")
    public ProfesionalVista activar(@PathVariable Long id) {
        return servicio.activar(id);
    }

    @PostMapping("/{id}/desactivar")
    public ProfesionalVista desactivar(@PathVariable Long id) {
        return servicio.desactivar(id);
    }

    @GetMapping("/{id}/prestaciones")
    public List<PrestacionVista> especialidades(@PathVariable Long id) {
        return servicio.especialidades(id);
    }

    @PutMapping("/{id}/prestaciones")
    public List<PrestacionVista> definirEspecialidades(@PathVariable Long id,
                                                       @Valid @RequestBody EspecialidadesSolicitud solicitud) {
        return servicio.definirEspecialidades(id, solicitud.prestacionIds());
    }

    @PutMapping("/{id}/sillon-habitual")
    public ProfesionalVista asignarSillonHabitual(@PathVariable Long id,
                                                  @RequestBody SillonHabitualSolicitud solicitud) {
        return servicio.asignarSillonHabitual(id, solicitud.sillonId());
    }
}
