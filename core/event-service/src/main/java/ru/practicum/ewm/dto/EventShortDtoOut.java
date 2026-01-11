package ru.practicum.ewm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import static ru.practicum.ewm.constants.Constants.DATE_TIME_FORMAT;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventShortDtoOut {

    Long id;
    String title;
    String annotation;
    CategoryDtoOut category;
    UserDtoOut initiator;

    @JsonFormat(pattern = DATE_TIME_FORMAT)
    LocalDateTime eventDate;

    Boolean paid;
    Integer confirmedRequests;

    Double rating;
}