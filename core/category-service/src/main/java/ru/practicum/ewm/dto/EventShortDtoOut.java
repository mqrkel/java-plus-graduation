package ru.practicum.ewm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.ewm.category.dto.CategoryDtoOut;
import ru.practicum.ewm.user.dto.UserDtoOut;

import static ru.practicum.ewm.constants.Constants.DATE_TIME_FORMAT;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventShortDtoOut {

    private Long id;
    private String title;
    private String annotation;
    private CategoryDtoOut category;
    private UserDtoOut initiator;

    @JsonFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime eventDate;

    private Boolean paid;
    private Integer confirmedRequests;

    private Double rating;
}