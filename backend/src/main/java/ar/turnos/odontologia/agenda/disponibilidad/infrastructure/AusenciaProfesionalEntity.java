package ar.turnos.odontologia.agenda.disponibilidad.infrastructure;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Ausencia o licencia de un profesional (V5, RN-AG-16). El motivo no se expone en turnos ni va a logs. */
@Entity
@Table(name = "ausencia_profesional")
class AusenciaProfesionalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long profesionalId;
    private String tipo;
    private Instant inicio;
    private Instant fin;
    private String motivo;
    private String registradoPor;
    private Instant registradoEn;

    protected AusenciaProfesionalEntity() {}

    AusenciaProfesionalEntity(Long profesionalId, String tipo, Instant inicio, Instant fin, String motivo,
                              String registradoPor, Instant registradoEn) {
        this.profesionalId = profesionalId;
        this.tipo = tipo;
        this.inicio = inicio;
        this.fin = fin;
        this.motivo = motivo;
        this.registradoPor = registradoPor;
        this.registradoEn = registradoEn;
    }

    Long id() {
        return id;
    }

    Long profesionalId() {
        return profesionalId;
    }

    String tipo() {
        return tipo;
    }

    Instant inicio() {
        return inicio;
    }

    Instant fin() {
        return fin;
    }

    String motivo() {
        return motivo;
    }

    String registradoPor() {
        return registradoPor;
    }

    Instant registradoEn() {
        return registradoEn;
    }
}
