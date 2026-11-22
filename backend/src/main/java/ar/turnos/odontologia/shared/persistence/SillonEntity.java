package ar.turnos.odontologia.shared.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "sillon")
public class SillonEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private boolean activo = true;

    protected SillonEntity() {}

    public SillonEntity(String nombre) { this.nombre = nombre; }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public boolean isActivo() { return activo; }

    // ---------- Modificaciones (C-04) ----------

    public void renombrar(String nombre) { this.nombre = nombre; }

    public void activar() { this.activo = true; }

    public void desactivar() { this.activo = false; }
}
