package ru.practicum.ewm.dto;


import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventInternalDto {
    Long id;
    Long initiatorId;
    EventState state;
    Integer participantLimit;
    Boolean requestModeration;
}