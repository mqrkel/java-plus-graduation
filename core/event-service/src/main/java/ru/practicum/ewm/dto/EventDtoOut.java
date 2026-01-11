package ru.practicum.ewm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.model.EventState;

import static ru.practicum.ewm.constants.Constants.DATE_TIME_FORMAT;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventDtoOut {
    Long id;
    String annotation;

    @JsonProperty("category")
    CategoryDtoOut categoryDto;

    Long confirmedRequests;

    @JsonFormat(pattern = DATE_TIME_FORMAT)
    LocalDateTime createdOn;

    String description;

    @JsonFormat(pattern = DATE_TIME_FORMAT)
    LocalDateTime eventDate;

    Long initiator;

    @JsonProperty("location")
    LocationDto locationDto;

    Boolean paid;
    Long participantLimit;

    @JsonFormat(pattern = DATE_TIME_FORMAT)
    LocalDateTime publishedOn;
    Boolean requestModeration;
    EventState state;
    String title;
    Double rating;
}