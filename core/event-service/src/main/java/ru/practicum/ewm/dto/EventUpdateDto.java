package ru.practicum.ewm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import static ru.practicum.ewm.constants.Constants.DATE_TIME_FORMAT;

@Getter
@Setter
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventUpdateDto {

    @Size(min = 3, max = 120, message = "The title length must be between 3 and 120 characters")
    String title;

    @Size(min = 20, max = 2000, message = "The annotation length must be between 20 and 2000 characters")
    String annotation;

    @JsonProperty("category_id")
    Long categoryId;

    @Size(min = 20, max = 7000, message = "The description length must be between 20 and 7000 characters")
    String description;

    @Future(message = "The event date must be in future")
    @JsonFormat(pattern = DATE_TIME_FORMAT)
    LocalDateTime eventDate;

    Long locationId;

    Boolean paid;

    @Min(0)
    Integer participantLimit;
    Boolean requestModeration;

    StateAction stateAction;

    public enum StateAction {
        SEND_TO_REVIEW,
        CANCEL_REVIEW
    }
}
