package ar.turnos.odontologia.agenda.disponibilidad.application;

import ar.turnos.odontologia.agenda.domain.reglas.FranjaHoraria;
import ar.turnos.odontologia.agenda.domain.reglas.HorarioSemanal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Horarios de trabajo de un profesional (RN-AG-13, design D7). Definirlos reemplaza el conjunto completo. El
 * dominio valida las franjas antes de tocar la base, y la base lo vuelve a validar con su {@code EXCLUDE}.
 */
@Service
@Transactional
public class ServicioHorarios {

    private final RegistroDisponibilidad registro;

    public ServicioHorarios(RegistroDisponibilidad registro) {
        this.registro = registro;
    }

    @Transactional(readOnly = true)
    public List<FranjaHoraria> consultar(Long profesionalId, Solicitante solicitante) {
        solicitante.exigirPropioSiEsOdontologo(profesionalId);
        exigirProfesional(profesionalId);
        return registro.franjas(profesionalId);
    }

    /** @throws ar.turnos.odontologia.agenda.domain.reglas.DisponibilidadException si las franjas se superponen */
    public List<FranjaHoraria> definir(Long profesionalId, List<FranjaHoraria> franjas) {
        exigirProfesional(profesionalId);
        HorarioSemanal.de(franjas);
        registro.reemplazarFranjas(profesionalId, franjas);
        return registro.franjas(profesionalId);
    }

    private void exigirProfesional(Long profesionalId) {
        if (!registro.existeProfesional(profesionalId)) {
            throw new RecursoNoEncontradoException("el profesional");
        }
    }
}
