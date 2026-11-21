package ar.turnos.odontologia.shared.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "prestacion")
public class PrestacionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private int duracionMinutos;
    private boolean activo = true;

    protected PrestacionEntity() {}

    public PrestacionEntity(String nombre, int duracionMinutos) {
        this.nombre = nombre;
        this.duracionMinutos = duracionMinutos;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public int getDuracionMinutos() { return duracionMinutos; }
    public boolean isActivo() { return activo; }
}