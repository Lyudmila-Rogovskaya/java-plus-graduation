package ru.practicum.main_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorWrapper handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream().map(error -> "Field: " + error.getField() + ". Error: " + error.getDefaultMessage() + ". Value: " + error.getRejectedValue()).collect(Collectors.joining("; "));

        log.warn("Ошибка валидации: {}", message);

        ApiError apiError = new ApiError("BAD_REQUEST", "Incorrectly made request.", message, LocalDateTime.now().format(FORMATTER));

        return new ApiErrorWrapper(apiError);
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorWrapper handleNotFoundException(NotFoundException e) {
        log.warn("Объект не найден: {}", e.getMessage());

        ApiError apiError = new ApiError("NOT_FOUND", "The required object was not found.",  // ← Стандартный reason
                e.getMessage(),  // ← Конкретное сообщение
                LocalDateTime.now().format(FORMATTER));

        return new ApiErrorWrapper(apiError);
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorWrapper handleConflictException(ConflictException e) {
        log.warn("Конфликт данных: {}", e.getMessage());

        String reason;
        String message = e.getMessage();

        if (message.contains("category") || message.contains("The category is not empty")) {
            reason = "For the requested operation the conditions are not met.";
        } else if (message.contains("could not execute statement") || message.contains("constraint")) {
            reason = "Integrity constraint has been violated.";
        } else {
            reason = "For the requested operation the conditions are not met.";
        }

        ApiError apiError = new ApiError("CONFLICT", reason, message, LocalDateTime.now().format(FORMATTER));

        return new ApiErrorWrapper(apiError);
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorWrapper handleValidationException(ValidationException e) {
        log.warn("Ошибка валидации : {}", e.getMessage());

        ApiError apiError = new ApiError("BAD_REQUEST", "Incorrectly made request.", e.getMessage(), LocalDateTime.now().format(FORMATTER));

        return new ApiErrorWrapper(apiError);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorWrapper handleMissingParams(MissingServletRequestParameterException e) {
        log.warn("Отсутствует обязательный параметр: {}", e.getMessage());

        ApiError apiError = new ApiError("BAD_REQUEST", "Incorrectly made request.", e.getMessage(), LocalDateTime.now().format(FORMATTER));

        return new ApiErrorWrapper(apiError);
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiErrorWrapper handleForbiddenException(ForbiddenException e) {
        log.warn("Доступ запрещен: {}", e.getMessage());

        ApiError apiError = new ApiError("FORBIDDEN", "For the requested operation the conditions are not met.", e.getMessage(), LocalDateTime.now().format(FORMATTER));

        return new ApiErrorWrapper(apiError);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorWrapper handleGenericException(Exception e) {
        log.error("Внутренняя ошибка сервера: {}", e.getMessage(), e);

        ApiError apiError = new ApiError("INTERNAL_SERVER_ERROR", "Internal server error", "Произошла непредвиденная ошибка", LocalDateTime.now().format(FORMATTER));

        return new ApiErrorWrapper(apiError);
    }
}