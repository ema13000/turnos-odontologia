package ar.turnos.odontologia.shared.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "profesional")
public class ProfesionalEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String apellido;
    private String matricula;
    private boolean activo = true;

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
}