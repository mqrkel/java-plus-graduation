package ru.practicum.statsserver.exception;

import lombok.Data;

@Data
public class ErrorResponse {
    private final String error;
}
