package ar.turnos.odontologia.shared.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "turno")
public class TurnoEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "profesional_id")
    private ProfesionalEntity profesional;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sillon_id")
    private SillonEntity sillon;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "prestacion_id")
    private PrestacionEntity prestacion;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    private PacienteEntity paciente;

    private Instant inicio;
    private Instant fin;
    private Instant finConMargen;
    private String estado = "CONFIRMADO";
    private boolean esSobreturno;
    private String motivoSobreturno;

    @Version
    private long version;

    protected TurnoEntity() {}

    public TurnoEntity(ProfesionalEntity profesional, SillonEntity sillon, PrestacionEntity prestacion,
                       PacienteEntity paciente, Instant inicio, Instant fin, Instant finConMargen) {
        this.profesional = profesional;
        this.sillon = sillon;
        this.prestacion = prestacion;
        this.paciente = paciente;
        this.inicio = inicio;
        this.fin = fin;
        this.finConMargen = finConMargen;
    }

    public Long getId() { return id; }
    public Instant getInicio() { return inicio; }
    public Instant getFin() { return fin; }
    public Instant getFinConMargen() { return finConMargen; }
    public String getEstado() { return estado; }
    public boolean isEsSobreturno() { return esSobreturno; }
    public String getMotivoSobreturno() { return motivoSobreturno; }
    public long getVersion() { return version; }
}