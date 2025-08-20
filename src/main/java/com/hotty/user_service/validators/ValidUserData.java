package com.hotty.user_service.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Constraint(validatedBy = UserDataModelValidator.class)
@Target({ ElementType.TYPE }) // Se aplica a nivel de clase
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidUserData {

    String message() default "Los datos del usuario contienen información inválida.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}