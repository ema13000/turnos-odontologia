package ar.turnos.odontologia.notificaciones.domain;

import java.util.Objects;

/** Nombre y dirección del consultorio que salen en el mensaje (un despliegue es un consultorio, DD-05). */
public record Consultorio(String nombre, String direccion) {

    public Consultorio {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(direccion, "direccion");
    }
}
