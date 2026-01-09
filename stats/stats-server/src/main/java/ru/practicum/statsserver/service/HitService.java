package ru.practicum.statsserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.statsdto.HitDto;
import ru.practicum.statsdto.StatsDtoOut;
import ru.practicum.statsserver.exception.ParameterInvalidException;
import ru.practicum.statsserver.mappers.HitMapper;
import ru.practicum.statsserver.mappers.StatsMapper;
import ru.practicum.statsserver.repository.HitRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HitService {
    private final HitRepository repository;

    public void add(HitDto hitDto) {
        repository.save(HitMapper.toHit(hitDto));
    }

    public Collection<StatsDtoOut> getStatistics(LocalDateTime start,
                                                 LocalDateTime end,
                                                 List<String> uris,
                                                 Boolean unique) {
        if (start.isAfter(end))
            throw new ParameterInvalidException("'start' date must be before the 'end' date");

        return repository.getStatistics(start, end, uris, unique).stream()
                .map(StatsMapper::toDto)
                .toList();
    }
}
