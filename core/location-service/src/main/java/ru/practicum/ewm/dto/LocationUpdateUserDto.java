package ru.practicum.ewm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LocationUpdateUserDto {

    String name;
    String address;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @JsonProperty(value = "lat")
    Double latitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @JsonProperty(value = "lon")
    Double longitude;
}
