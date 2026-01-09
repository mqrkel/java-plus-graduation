package ru.practicum.statsserver.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
