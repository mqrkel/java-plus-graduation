package ru.practicum.ewm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import static ru.practicum.ewm.constants.Constants.DATE_TIME_FORMAT;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventCreateDto {

    @NotBlank
    @Size(min = 3, max = 120, message = "The title length must be between 3 and 120 characters")
    String title;

    @NotBlank
    @Size(min = 20, max = 2000, message = "The annotation length must be between 20 and 2000 characters")
    String annotation;

    @NotNull(message = "Category cannot be null")
    @JsonProperty("category")
    Long categoryId;

    @NotBlank
    @Size(min = 20, max = 7000, message = "The description length must be between 20 and 7000 characters")
    String description;

    @NotNull(message = "The event date cannot be empty")
    @Future(message = "The event date must be in future")
    @JsonFormat(pattern = DATE_TIME_FORMAT)
    LocalDateTime eventDate;

    @NotNull(message = "Location cannot be null")
    LocationDto location;

    Boolean paid = false;

    @Min(0)
    Integer participantLimit = 0;
    Boolean requestModeration = true;
}