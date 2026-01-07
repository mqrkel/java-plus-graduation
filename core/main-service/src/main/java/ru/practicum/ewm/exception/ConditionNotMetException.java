package ru.practicum.ewm.exception;

public class ConditionNotMetException extends RuntimeException {
    public ConditionNotMetException(String s) {
        super(s);
    }
}
