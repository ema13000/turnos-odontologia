package ar.turnos.odontologia.usuarios.api;

import ar.turnos.odontologia.usuarios.domain.PoliticaContrasena;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** La contraseña cumple la política del dominio (8 a 72 caracteres). */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ContrasenaValida.Validador.class)
@interface ContrasenaValida {

    String message() default "La contraseña debe tener entre 8 y 72 caracteres";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<ContrasenaValida, String> {
        @Override
        public boolean isValid(String valor, ConstraintValidatorContext contexto) {
            // el valor nulo o en blanco lo informa @NotBlank
            return valor == null || valor.isBlank() || PoliticaContrasena.cumple(valor);
        }
    }
}
