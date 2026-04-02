package ru.practicum.main_service.event.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ModerationCommentValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidModerationComment {

    String message() default "Комментарий модерации должен быть от 10 до 1000 символов";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
