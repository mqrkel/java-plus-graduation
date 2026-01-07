package ru.practicum.ewm.location.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class LocationCreateDto {

    @NotBlank(message = "The location name cannot be blank")
    @Size(min = 4, max = 64, message = "The location name length must be between 4 and 64 characters")
    private String name;

    private String address;

    @NotNull(message = "The location latitude cannot be empty")
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @JsonProperty(value = "lat")
    private Double latitude;

    @NotNull(message = "The location longitude cannot be empty")
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @JsonProperty(value = "lon")
    private Double longitude;
}
