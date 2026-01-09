package ru.practicum.ewm.location.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {

    Long id;

    @DecimalMin("-90.0") @DecimalMax("90.0")
    @JsonProperty(value = "lat")
    private Double latitude;

    @DecimalMin("-180.0") @DecimalMax("180.0")
    @JsonProperty(value = "lon")
    private Double longitude;
}