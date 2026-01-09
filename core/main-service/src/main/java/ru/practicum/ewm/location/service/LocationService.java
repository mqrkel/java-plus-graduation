package ru.practicum.ewm.location.service;

import ru.practicum.ewm.location.dto.*;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.location.model.LocationAdminFilter;
import ru.practicum.ewm.location.model.LocationPrivateFilter;
import ru.practicum.ewm.location.model.LocationPublicFilter;

import java.util.Collection;

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
}
