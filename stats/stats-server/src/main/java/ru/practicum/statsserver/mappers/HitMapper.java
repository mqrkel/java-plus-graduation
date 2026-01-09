package ru.practicum.statsserver.mappers;

import lombok.experimental.UtilityClass;
import ru.practicum.statsdto.HitDto;
import ru.practicum.statsserver.model.Hit;

@UtilityClass
public class HitMapper {
    public Hit toHit(HitDto dto) {
        Hit hit = new Hit();
        hit.setService(dto.getService());
        hit.setUri(dto.getUri());
        hit.setIp(dto.getIp());
        hit.setDateTime(dto.getDateTime());
        return hit;
    }
}
