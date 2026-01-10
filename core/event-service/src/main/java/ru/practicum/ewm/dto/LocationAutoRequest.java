package ru.practicum.ewm.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LocationAutoRequest {

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    Double latitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    Double longitude;
}