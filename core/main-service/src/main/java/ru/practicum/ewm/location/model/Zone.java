package ru.practicum.ewm.location.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Zone {
    private Double latitude;
    private Double longitude;
    private Double radius;
}
