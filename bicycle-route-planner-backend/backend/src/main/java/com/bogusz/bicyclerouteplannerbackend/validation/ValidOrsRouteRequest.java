package com.bogusz.bicyclerouteplannerbackend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = OrsRouteRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidOrsRouteRequest {
    String message() default "Invalid route request coordinates or parameters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
