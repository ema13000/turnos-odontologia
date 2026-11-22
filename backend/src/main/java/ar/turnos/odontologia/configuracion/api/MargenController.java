package ar.turnos.odontologia.configuracion.api;

import ar.turnos.odontologia.configuracion.api.Solicitudes.MargenDto;
import ar.turnos.odontologia.configuracion.application.MargenService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Margen entre turnos del consultorio (RN-AG-04). */
@RestController
@RequestMapping("/api/v1/configuracion/margen")
public class MargenController {

    /** Solo ADMIN modifica la configuración; el resto del personal consulta (matriz RBAC de la KB). */
    private static final String SOLO_ADMIN = "hasRole('ADMIN')";

    private final MargenService servicio;

    public MargenController(MargenService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public MargenDto obtener() {
        return new MargenDto((int) servicio.minutos());
    }

    @PreAuthorize(SOLO_ADMIN)
    @PutMapping
    public MargenDto cambiar(@RequestBody MargenDto margen) {
        return new MargenDto((int) servicio.cambiar(margen.minutos()));
    }
}
