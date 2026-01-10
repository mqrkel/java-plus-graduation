package ru.practicum.ewm.dto;

import lombok.Data;

@Data
public class EventInternalDto {
    private Long id;
    private Long initiatorId;
    private EventState state;
    private Integer participantLimit;
    private Boolean requestModeration;
}