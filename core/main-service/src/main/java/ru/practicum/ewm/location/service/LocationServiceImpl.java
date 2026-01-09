package ru.practicum.ewm.location.service;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConditionNotMetException;
import ru.practicum.ewm.exception.DuplicateLocationsException;
import ru.practicum.ewm.exception.NoAccessException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.location.dto.*;
import ru.practicum.ewm.location.mapper.LocationMapper;
import ru.practicum.ewm.location.model.*;
import ru.practicum.ewm.location.repository.LocationRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationServiceImpl implements LocationService {

    private static final double NEARBY_RADIUS = 50; // meters

    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final EventRepository eventRepository;


    @Override
    @Transactional
    public LocationFullDtoOut addLocationByAdmin(LocationCreateDto dto) {
        Location location = LocationMapper.fromDto(dto);
        location.setState(LocationState.APPROVED);
        return LocationMapper.toFullDto(locationRepository.save(location));
    }

    @Override
    @Transactional
    public LocationPrivateDtoOut addLocation(Long userId, LocationCreateDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));

        checkForDuplicate(dto.getName(), dto.getLatitude(), dto.getLongitude());

        Location location = LocationMapper.fromDto(dto);
        location.setCreator(user);
        Location saved = locationRepository.save(location);
        return LocationMapper.toPrivateDto(saved);
    }

    @Override
    @Transactional
    public LocationFullDtoOut update(Long id, LocationUpdateAdminDto dto) {
        log.debug("try update location by admin: {}", dto);
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location", id));

        Optional.ofNullable(dto.getName()).ifPresent(location::setName);
        Optional.ofNullable(dto.getAddress()).ifPresent(location::setAddress);
        Optional.ofNullable(dto.getLatitude()).ifPresent(location::setLatitude);
        Optional.ofNullable(dto.getLongitude()).ifPresent(location::setLongitude);
        Optional.ofNullable(dto.getState()).ifPresent(
                state -> changeLocationState(location, state));

        return LocationMapper.toFullDto(location);
    }

    @Override
    @Transactional
    public LocationPrivateDtoOut update(Long id, Long userId, LocationUpdateUserDto dto) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location", id));

        if (location.getState() != LocationState.PENDING) {
            throw new ConditionNotMetException("Cannot update published or rejected location");
        }

        if (location.getCreator() == null || !location.getCreator().getId().equals(userId)) {
            throw new NoAccessException("Only creator can edit this location");
        }

        boolean needToCheckDuplicates =
                (dto.getName() != null && !dto.getName().equals(location.getName())) ||
                (dto.getLatitude() != null && !dto.getLatitude().equals(location.getLatitude())) ||
                (dto.getLongitude() != null && !dto.getLongitude().equals(location.getLongitude()));

        if (needToCheckDuplicates) {
            final String name = Optional.ofNullable(dto.getName()).orElse(location.getName());
            final Double lat = Optional.ofNullable(dto.getLatitude()).orElse(location.getLatitude());
            final Double lon = Optional.ofNullable(dto.getLongitude()).orElse(location.getLongitude());
            checkForDuplicate(name, lat, lon);
        }

        Optional.ofNullable(dto.getName()).ifPresent(location::setName);
        Optional.ofNullable(dto.getAddress()).ifPresent(location::setAddress);
        Optional.ofNullable(dto.getLatitude()).ifPresent(location::setLatitude);
        Optional.ofNullable(dto.getLongitude()).ifPresent(location::setLongitude);

        return LocationMapper.toPrivateDto(location);
    }

    private void checkForDuplicate(String name, Double lat, Double lon) {
        log.debug("need check  for duplicates");
        Optional<Location> existing = locationRepository.findDuplicates(name, lat, lon, NEARBY_RADIUS);

        if (existing.isPresent()) {
            log.warn("Nearby location: {}", existing.get());
            throw new DuplicateLocationsException(getDuplicateErrorMessage(existing.get()));
        }
    }

    @Override
    public LocationDtoOut getApproved(Long id) {
        Location location = locationRepository.findByIdAndState(id, LocationState.APPROVED)
                .orElseThrow(() -> new NotFoundException("Location", id));

        return LocationMapper.toDto(location);
    }


    private void changeLocationState(Location location, LocationState state) {
        log.debug("changeLocationState id:{} state: {} -> {}", location.getId(), location.getState(), state);
        if (location.getState() == state)
            return;

        if (state == LocationState.PENDING || state == LocationState.AUTO_GENERATED) {
            throw new ConditionNotMetException(
                    String.format("Cannot change state from %s to %s", location.getState(), state));
        }
        location.setState(state);
    }

    private static String getDuplicateErrorMessage(@NotNull Location existing) {
        Long id = existing.getId();
        switch (existing.getState()) {
            case LocationState.APPROVED -> {
                return String.format("Please use existing location (id=%d)", id);
            }
            case LocationState.PENDING -> {
                return String.format("A request to create this location already exists (id=%d). Please wait for approval.", id);
            }
            case LocationState.REJECTED -> {
                return  "The request for creating this location was rejected earlier. Please contact admin.";
            }
        }
        return "";
    }

    @Override
    public Collection<LocationFullDtoOut> findAllByFilter(LocationAdminFilter filter) {
        Specification<Location> spec = buildSpecification(filter);
        List<Location> locations = locationRepository.findAll(spec, filter.getPageable()).getContent();
        return locations.stream()
                .map(LocationMapper::toFullDto)
                .toList();
    }

    @Override
    public Collection<LocationPrivateDtoOut> findAllByFilter(Long userId, LocationPrivateFilter filter) {

        if (!userRepository.existsById(userId))
            throw new NotFoundException("User", userId);

        Specification<Location> spec = buildSpecification(userId, filter);
        List<Location> locations = locationRepository.findAll(spec, filter.getPageable()).getContent();
        return locations.stream()
                .map(LocationMapper::toPrivateDto)
                .toList();
    }

    @Override
    public Collection<LocationDtoOut> findAllByFilter(LocationPublicFilter filter) {
        Specification<Location> spec = buildSpecification(filter);
        List<Location> locations = locationRepository.findAll(spec, filter.getPageable()).getContent();
        return locations.stream()
                .map(LocationMapper::toDto)
                .toList();
    }

    @Override
    public LocationFullDtoOut getByIdForAdmin(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location", id));
        return LocationMapper.toFullDto(location);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (eventRepository.existsByLocationId(id)) {
            throw new ConditionNotMetException("There are events in this location");
        }
        locationRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location", id));

        if (location.getState() == LocationState.APPROVED) {
            throw new ConditionNotMetException("Cannot delete published location");
        }

        if (location.getCreator() == null || !location.getCreator().getId().equals(userId)) {
            throw new NoAccessException("Only creator can delete this location");
        }

        if (eventRepository.existsByLocationId(id)) {
            throw new ConditionNotMetException("There are events in this location");
        }

        locationRepository.deleteById(id);
    }

    @Override
    public Location getOrCreateLocation(LocationDto location) {

        if (location.getId() != null) {
            return locationRepository.findByIdAndState(location.getId(), LocationState.APPROVED)
                    .orElseThrow(() -> new NotFoundException("Location", location.getId()));
        }

        if (location.getLatitude() != null && location.getLongitude() != null) {
            Optional<Location> nearByAutoGenerated = locationRepository.findNearByAutoGenerated(
                    location.getLatitude(), location.getLongitude());

            return nearByAutoGenerated.orElseGet(()
                    -> createAutoGeneratedLocation(location.getLatitude(), location.getLongitude()));
        }

        throw new ConditionNotMetException("Invalid location");
    }

    @Transactional
    public Location createAutoGeneratedLocation(Double lat, Double lon) {
        Location location = Location.builder()
                .latitude(lat)
                .longitude(lon)
                .state(LocationState.AUTO_GENERATED)
                .build();
        return locationRepository.save(location);
    }

    private Specification<Location> buildSpecification(LocationAdminFilter filter) {
        return Stream.of(
                        optionalSpec(LocationSpecifications.withTextContains(filter.getText())),
                        optionalSpec(LocationSpecifications.withCreator(filter.getCreator())),
                        optionalSpec(LocationSpecifications.withCoordinates(filter.getZone())),
                        optionalSpec(LocationSpecifications.withState(filter.getState())),
                        optionalSpec(LocationSpecifications.withEventsCount(filter.getMinEvents(), filter.getMaxEvents()))
                )
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }

    private Specification<Location> buildSpecification(Long userId, LocationPrivateFilter filter) {
        return Stream.of(
                        optionalSpec(LocationSpecifications.withCreator(userId)),
                        optionalSpec(LocationSpecifications.withState(filter.getState())),
                        optionalSpec(LocationSpecifications.withTextContains(filter.getText())),
                        optionalSpec(LocationSpecifications.withCoordinates(filter.getZone()))
                )
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }

    private Specification<Location> buildSpecification(LocationPublicFilter filter) {
        return Stream.of(
                        optionalSpec(LocationSpecifications.withState(LocationState.APPROVED)),
                        optionalSpec(LocationSpecifications.withTextContains(filter.getText())),
                        optionalSpec(LocationSpecifications.withCoordinates(filter.getZone()))
                )
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }

    private static <T> Specification<T> optionalSpec(Specification<T> spec) {
        return spec;
    }
}
