package ru.practicum.ewm.compilation.mapper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.dto.EventShortDtoOut;

@UtilityClass
public class CompilationMapper {

    public Compilation toEntity(NewCompilationDto dto) {
        Set<Long> eventIds = dto.getEvents() != null
                ? new HashSet<>(dto.getEvents())
                : new HashSet<>();

        return Compilation.builder()
                .title(dto.getTitle())
                .pinned(Boolean.TRUE.equals(dto.getPinned()))
                .events(eventIds)
                .build();
    }

    public CompilationDto toDto(
            Compilation compilation,
            Map<Long, EventShortDtoOut> eventsById
    ) {
        Set<EventShortDtoOut> events = compilation.getEvents() == null
                ? Collections.emptySet()
                : compilation.getEvents().stream()
                .map(eventsById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(events)
                .build();
    }
}