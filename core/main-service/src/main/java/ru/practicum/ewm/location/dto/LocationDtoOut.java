package ru.practicum.ewm.location.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class LocationDtoOut {
    private Long id;
    private String name;
    private String address;

    @JsonProperty(value = "lat")
    private Double latitude;
    @JsonProperty(value = "lon")
    private Double longitude;
}
