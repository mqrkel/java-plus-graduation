package ru.practicum.ewm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import ru.practicum.ewm.category.dto.CategoryDtoOut;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.location.dto.LocationDtoOut;
import ru.practicum.ewm.user.dto.UserDtoOut;

import java.time.LocalDateTime;

import static ru.practicum.ewm.constants.Constants.DATE_TIME_FORMAT;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EventDtoOut {

    private Long id;
    private String title;
    private String annotation;
    private String description;
    private CategoryDtoOut category;
    private UserDtoOut initiator;
    private LocationDtoOut location;

    @JsonFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime eventDate;

    @JsonFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime createdOn;

    @JsonFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime publishedOn;

    private Boolean paid;
    private Integer participantLimit;
    private Boolean requestModeration;
    private EventState state;
    private Integer confirmedRequests;

    @Builder.Default
    private Integer views = 0;
}