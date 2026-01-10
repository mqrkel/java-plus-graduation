package ru.practicum.user.exception;

public class NoAccessException extends RuntimeException {
    public NoAccessException(String s) {
        super(s);
    }
}
