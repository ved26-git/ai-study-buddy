package com.aistudybuddy.dto.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that question count is restricted to 5, 10, or 15.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidQuestionCountValidator.class)
@Documented
public @interface ValidQuestionCount {
    String message() default "Question count must be exactly 5, 10, or 15";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
