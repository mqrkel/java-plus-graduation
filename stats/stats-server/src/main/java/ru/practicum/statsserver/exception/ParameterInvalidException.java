package ru.practicum.statsserver.exception;

public class ParameterInvalidException extends RuntimeException {
    public ParameterInvalidException(String message) {
        super(message);
    }
}
