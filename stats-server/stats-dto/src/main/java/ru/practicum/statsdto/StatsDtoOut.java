package ru.practicum.statsdto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class StatsDtoOut {

    @JsonProperty("app")
    private String service;
    private String uri;
    private int hits;
}
