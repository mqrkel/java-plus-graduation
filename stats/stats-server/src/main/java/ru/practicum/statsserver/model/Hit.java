package ru.practicum.statsserver.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Hit {
    private String service;
    private String uri;
    private String ip;
    private LocalDateTime dateTime;
}
