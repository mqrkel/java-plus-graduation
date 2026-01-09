package ru.practicum.ewm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.location.dto.LocationDto;

import java.time.LocalDateTime;

import static ru.practicum.ewm.constants.Constants.DATE_TIME_FORMAT;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventCreateDto {

    @NotBlank
    @Size(min = 3, max = 120, message = "The title length must be between 3 and 120 characters")
    private String title;

    @NotBlank
    @Size(min = 20, max = 2000, message = "The annotation length must be between 20 and 2000 characters")
    private String annotation;

    @NotNull(message = "Category cannot be null")
    @JsonProperty("category")
    private Long categoryId;

    @NotBlank
    @Size(min = 20, max = 7000, message = "The description length must be between 20 and 7000 characters")
    private String description;

    @NotNull(message = "The event date cannot be empty")
    @Future(message = "The event date must be in future")
    @JsonFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime eventDate;

    @NotNull(message = "Location cannot be null")
    private LocationDto location;

    private Boolean paid = false;

    @Min(0)
    private Integer participantLimit = 0;
    private Boolean requestModeration = true;
}