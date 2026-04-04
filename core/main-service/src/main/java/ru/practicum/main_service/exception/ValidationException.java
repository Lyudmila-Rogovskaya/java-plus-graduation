package ru.practicum.main_service.exception; // перенесла в юзер-сервис

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}