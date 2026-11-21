package ar.turnos.odontologia.agenda.domain;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Reglas de conflicto de la agenda (RN-AG-02 a RN-AG-05).
 *
 * <p>Si hay más de un conflicto, informa uno solo con prioridad fija:
 * {@code SOLAPAMIENTO_PROFESIONAL}, después {@code SOLAPAMIENTO_SILLON} y por último
 * {@code MARGEN_INSUFICIENTE}. Así el resultado no depende del orden de los turnos.
 */
public final class Agenda {

    private final Margen margen;

    public Agenda(Margen margen) {
        this.margen = Objects.requireNonNull(margen, "margen");
    }

    /**
     * Lanza {@link TurnoRechazadoException} si el turno nuevo entra en conflicto con algún
     * turno activo existente que comparte profesional o sillón.
     */
    public void verificar(Turno nuevo, Collection<Turno> existentes) {
        buscarConflicto(nuevo, existentes).ifPresent(codigo -> {
            throw new TurnoRechazadoException(codigo, mensajePara(codigo));
        });
    }

    Optional<CodigoRechazo> buscarConflicto(Turno nuevo, Collection<Turno> existentes) {
        boolean solapaProfesional = false;
        boolean solapaSillon = false;
        boolean faltaMargen = false;

        for (Turno existente : existentes) {
            if (!existente.esActivo()) {
                continue;
            }
            boolean compartePorProfesional = nuevo.mismoProfesionalQue(existente);
            boolean compartePorSillon = nuevo.mismoSillonQue(existente);
            if (!compartePorProfesional && !compartePorSillon) {
                continue;
            }
            if (nuevo.intervalo().seSuperponeCon(existente.intervalo())) {
                solapaProfesional |= compartePorProfesional;
                solapaSillon |= compartePorSillon;
            } else if (nuevo.intervalo().seAcercaA(existente.intervalo(), margen)) {
                faltaMargen = true;
            }
        }

        if (solapaProfesional) {
            return Optional.of(CodigoRechazo.SOLAPAMIENTO_PROFESIONAL);
        }
        if (solapaSillon) {
            return Optional.of(CodigoRechazo.SOLAPAMIENTO_SILLON);
        }
        if (faltaMargen) {
            return Optional.of(CodigoRechazo.MARGEN_INSUFICIENTE);
        }
        return Optional.empty();
    }

    private String mensajePara(CodigoRechazo codigo) {
        return switch (codigo) {
            case SOLAPAMIENTO_PROFESIONAL -> "El profesional ya tiene un turno en ese horario";
            case SOLAPAMIENTO_SILLON -> "El sillón ya está ocupado en ese horario";
            case MARGEN_INSUFICIENTE -> "No queda el margen de " + margen.valor().toMinutes()
                    + " minutos con otro turno del mismo profesional o sillón";
            default -> codigo.name();
        };
    }
}
