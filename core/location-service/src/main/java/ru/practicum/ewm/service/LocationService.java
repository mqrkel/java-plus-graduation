package ru.practicum.ewm.service;

import java.util.Collection;
import ru.practicum.ewm.dto.LocationAutoRequest;
import ru.practicum.ewm.dto.LocationCreateDto;
import ru.practicum.ewm.dto.LocationDto;
import ru.practicum.ewm.dto.LocationDtoOut;
import ru.practicum.ewm.dto.LocationFullDtoOut;
import ru.practicum.ewm.dto.LocationPrivateDtoOut;
import ru.practicum.ewm.dto.LocationUpdateAdminDto;
import ru.practicum.ewm.dto.LocationUpdateUserDto;
import ru.practicum.ewm.model.Location;
import ru.practicum.ewm.model.LocationAdminFilter;
import ru.practicum.ewm.model.LocationPrivateFilter;
import ru.practicum.ewm.model.LocationPublicFilter;

public interface LocationService {

    LocationPrivateDtoOut addLocation(Long userId, LocationCreateDto dto);

    LocationFullDtoOut addLocationByAdmin(LocationCreateDto dto);

    LocationFullDtoOut update(Long id, LocationUpdateAdminDto dto);

    LocationPrivateDtoOut update(Long id, Long userId, LocationUpdateUserDto dto);

    LocationDtoOut getApproved(Long id);

    Collection<LocationFullDtoOut> findAllByFilter(LocationAdminFilter filter);

    Collection<LocationPrivateDtoOut> findAllByFilter(Long userId, LocationPrivateFilter filter);

    Collection<LocationDtoOut> findAllByFilter(LocationPublicFilter filter);

    void delete(Long id);

    void delete(Long id, Long userId);

    Location getOrCreateLocation(LocationDto location);

    LocationFullDtoOut getByIdForAdmin(Long id);

    LocationDtoOut getOrCreateAuto(LocationAutoRequest request);
}
