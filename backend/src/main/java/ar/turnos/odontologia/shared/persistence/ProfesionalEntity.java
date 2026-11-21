package ar.turnos.odontologia.shared.persistence;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "profesional")
public class ProfesionalEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String apellido;
    private String matricula;
    private boolean activo = true;

    // C-04: sillón habitual (US-018), opcional.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sillon_habitual_id")
    private SillonEntity sillonHabitual;

    // C-04: especialidades, las prestaciones que hace el profesional (RN-AG-15).
    @ManyToMany
    @JoinTable(name = "profesional_prestacion",
            joinColumns = @JoinColumn(name = "profesional_id"),
            inverseJoinColumns = @JoinColumn(name = "prestacion_id"))
    private Set<PrestacionEntity> especialidades = new HashSet<>();

    protected ProfesionalEntity() {}

    public ProfesionalEntity(String nombre, String apellido, String matricula) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.matricula = matricula;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getMatricula() { return matricula; }
    public boolean isActivo() { return activo; }
    public SillonEntity getSillonHabitual() { return sillonHabitual; }
    public Set<PrestacionEntity> getEspecialidades() { return Set.copyOf(especialidades); }

    // ---------- Modificaciones (C-04) ----------

    public void actualizar(String nombre, String apellido, String matricula) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.matricula = matricula;
    }

    public void activar() { this.activo = true; }

    public void desactivar() { this.activo = false; }

    /** Asigna el sillón habitual, o lo quita si es {@code null}. */
    public void asignarSillonHabitual(SillonEntity sillon) { this.sillonHabitual = sillon; }

    /** Reemplaza el conjunto completo de especialidades. */
    public void reemplazarEspecialidades(Set<PrestacionEntity> prestaciones) {
        this.especialidades.clear();
        this.especialidades.addAll(prestaciones);
    }
}
