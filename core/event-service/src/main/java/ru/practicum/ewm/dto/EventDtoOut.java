package ru.practicum.ewm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    String title;
    String annotation;
    String description;
    CategoryDtoOut category;
    UserDtoOut initiator;
    LocationDto location;

    @JsonFormat(pattern = DATE_TIME_FORMAT)
    LocalDateTime eventDate;

    @JsonFormat(pattern = DATE_TIME_FORMAT)
    LocalDateTime createdOn;

    @JsonFormat(pattern = DATE_TIME_FORMAT)
    LocalDateTime publishedOn;

    Boolean paid;
    Integer participantLimit;
    Boolean requestModeration;
    EventState state;
    Integer confirmedRequests;

    @Builder.Default
    Integer views = 0;
}