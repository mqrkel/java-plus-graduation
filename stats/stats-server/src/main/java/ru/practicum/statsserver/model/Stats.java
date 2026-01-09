package ru.practicum.statsserver.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class Stats {
    private String service;
    private String uri;
    private int hits;
}
