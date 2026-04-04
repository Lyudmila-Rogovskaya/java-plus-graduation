package ru.practicum.main_service.exception; // перенесла в юзер-сервис

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
