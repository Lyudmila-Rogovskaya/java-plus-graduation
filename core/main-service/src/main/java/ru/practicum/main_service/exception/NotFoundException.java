package ru.practicum.main_service.exception; // перенесла в юзер-сервис и в эвент-сервис

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}