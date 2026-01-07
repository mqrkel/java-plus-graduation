package ru.practicum.statsserver.mappers;

import lombok.experimental.UtilityClass;
import ru.practicum.statsdto.StatsDtoOut;
import ru.practicum.statsserver.model.Stats;

@UtilityClass
public class StatsMapper {
    public StatsDtoOut toDto(Stats stats) {
        StatsDtoOut dto = new StatsDtoOut();
        dto.setService(stats.getService());
        dto.setUri(stats.getUri());
        dto.setHits(stats.getHits());
        return dto;
    }
}
