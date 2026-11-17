package com.aistudybuddy.dto.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

/**
 * ConstraintValidator implementation enforcing that question counts are strictly 5, 10, or 15.
 */
public class ValidQuestionCountValidator implements ConstraintValidator<ValidQuestionCount, Integer> {

    private static final Set<Integer> ALLOWED_COUNTS = Set.of(5, 10, 15);

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return ALLOWED_COUNTS.contains(value);
    }
}
