package ar.turnos.odontologia.agenda.disponibilidad.infrastructure;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Bloqueo de la agenda de un profesional (V5, RN-AG-10). */
@Entity
@Table(name = "bloqueo_agenda")
class BloqueoAgendaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long profesionalId;
    private Instant inicio;
    private Instant fin;
    private String motivo;
    private String registradoPor;
    private Instant registradoEn;

    protected BloqueoAgendaEntity() {}

    BloqueoAgendaEntity(Long profesionalId, Instant inicio, Instant fin, String motivo, String registradoPor,
                        Instant registradoEn) {
        this.profesionalId = profesionalId;
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
