package ru.practicum.event_service.exception;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

}
