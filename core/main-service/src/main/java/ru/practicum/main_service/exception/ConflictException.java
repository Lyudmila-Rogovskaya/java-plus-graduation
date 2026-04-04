package ru.practicum.main_service.exception; // /перенесла в юзер-сервис и в эвент-сервис

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

}
