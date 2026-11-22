package ar.turnos.odontologia.agenda.disponibilidad.infrastructure;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;

/** Franja semanal de trabajo (V5). {@code diaSemana} sigue ISO-8601: 1 = lunes … 7 = domingo. */
@Entity
@Table(name = "horario_trabajo")
class HorarioTrabajoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long profesionalId;
    private Short diaSemana;
    private LocalTime horaDesde;
    private LocalTime horaHasta;

    protected HorarioTrabajoEntity() {}

    HorarioTrabajoEntity(Long profesionalId, int diaSemana, LocalTime horaDesde, LocalTime horaHasta) {
        this.profesionalId = profesionalId;
        this.diaSemana = (short) diaSemana;
        this.horaDesde = horaDesde;
        this.horaHasta = horaHasta;
    }

    Long profesionalId() {
        return profesionalId;
    }

    int diaSemana() {
        return diaSemana;
    }

    LocalTime horaDesde() {
        return horaDesde;
    }

    LocalTime horaHasta() {
        return horaHasta;
    }
}
