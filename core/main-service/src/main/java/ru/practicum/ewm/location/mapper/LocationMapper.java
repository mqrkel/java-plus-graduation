package ru.practicum.ewm.location.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.location.dto.LocationCreateDto;
import ru.practicum.ewm.location.dto.LocationDtoOut;
import ru.practicum.ewm.location.dto.LocationFullDtoOut;
import ru.practicum.ewm.location.dto.LocationPrivateDtoOut;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.user.mapper.UserMapper;

@UtilityClass
public class LocationMapper {
    public static Location fromDto(LocationCreateDto dto) {
        return Location.builder()
                .name(dto.getName())
                .address(dto.getAddress())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .build();
    }

    public static LocationDtoOut toDto(Location location) {
        return LocationDtoOut.builder()
                .id(location.getId())
                .name(location.getName())
                .address(location.getAddress())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
    }

    public static LocationFullDtoOut toFullDto(Location location) {
        return LocationFullDtoOut.builder()
                .id(location.getId())
                .name(location.getName())
                .address(location.getAddress())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .creator(location.getCreator() == null ? null : UserMapper.toDto(location.getCreator()))
                .state(location.getState())
                .build();
    }

    public static LocationPrivateDtoOut toPrivateDto(Location location) {
        return LocationPrivateDtoOut.builder()
                .id(location.getId())
                .name(location.getName())
                .address(location.getAddress())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .state(location.getState())
                .build();
    }
}
