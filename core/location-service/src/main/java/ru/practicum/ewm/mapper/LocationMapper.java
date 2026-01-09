package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.dto.LocationCreateDto;
import ru.practicum.ewm.dto.LocationDtoOut;
import ru.practicum.ewm.dto.LocationFullDtoOut;
import ru.practicum.ewm.dto.LocationPrivateDtoOut;
import ru.practicum.ewm.model.Location;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    // === CreateDto → Entity ===
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @Mapping(target = "state", ignore = true)
    Location fromCreateDto(LocationCreateDto dto);

    // === Entity → DTO (public) ===
    LocationDtoOut toDto(Location location);

    // === Entity → DTO (full admin) ===
    LocationFullDtoOut toFullDto(Location location);

    // === Entity → DTO (private) ===
    LocationPrivateDtoOut toPrivateDto(Location location);
}