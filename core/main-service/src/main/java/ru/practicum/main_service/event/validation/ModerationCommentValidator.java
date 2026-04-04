package ru.practicum.main_service.event.validation; // перенесла в эвент-сервис

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ModerationCommentValidator implements ConstraintValidator<ValidModerationComment, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if (value == null) {
            return true;
        }

        String trimmed = value.trim();
        return trimmed.length() >= 10 && trimmed.length() <= 1000;
    }

}
