package ru.practicum.ewm.location.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import ru.practicum.ewm.location.model.LocationState;
import ru.practicum.ewm.user.dto.UserDtoOut;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class LocationFullDtoOut {
    private Long id;
    private String name;
    private String address;
    @JsonProperty(value = "lat")
    private Double latitude;
    @JsonProperty(value = "lon")
    private Double longitude;
    private UserDtoOut creator;
    private LocationState state;
}
