package ar.turnos.odontologia.configuracion.infrastructure;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Par clave y valor de la configuración del consultorio (tabla {@code configuracion}, V2). */
@Entity
@Table(name = "configuracion")
public class ConfiguracionEntity {

    @Id
    private String clave;
    private String valor;

    protected ConfiguracionEntity() {
    }

    public ConfiguracionEntity(String clave, String valor) {
        this.clave = clave;
        this.valor = valor;
    }

    public String getClave() { return clave; }
    public String getValor() { return valor; }

    public void cambiarValor(String valor) { this.valor = valor; }
}
