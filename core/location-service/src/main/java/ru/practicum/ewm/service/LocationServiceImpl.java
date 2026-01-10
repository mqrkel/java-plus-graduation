package ru.practicum.ewm.service;

import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.practicum.ewm.client.EventClient;
import ru.practicum.ewm.client.UserClient;
import ru.practicum.ewm.dto.LocationAutoRequest;
import ru.practicum.ewm.dto.LocationCreateDto;
import ru.practicum.ewm.dto.LocationDto;
import ru.practicum.ewm.dto.LocationDtoOut;
import ru.practicum.ewm.dto.LocationFullDtoOut;
import ru.practicum.ewm.dto.LocationPrivateDtoOut;
import ru.practicum.ewm.dto.LocationUpdateAdminDto;
import ru.practicum.ewm.dto.LocationUpdateUserDto;
import ru.practicum.ewm.exception.ConditionNotMetException;
import ru.practicum.ewm.exception.DuplicateLocationsException;
import ru.practicum.ewm.exception.NoAccessException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.LocationMapper;
import ru.practicum.ewm.model.Location;
import ru.practicum.ewm.model.LocationAdminFilter;
import ru.practicum.ewm.model.LocationPrivateFilter;
import ru.practicum.ewm.model.LocationPublicFilter;
import ru.practicum.ewm.model.LocationState;
import ru.practicum.ewm.repository.LocationRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private static final double NEARBY_RADIUS = 50; // meters

    private final UserClient userClient;
    private final LocationRepository locationRepository;
    private final EventClient eventClient;
    private final TransactionTemplate transactionTemplate;
    private final LocationMapper locationMapper;

    @Override
    public LocationFullDtoOut addLocationByAdmin(LocationCreateDto dto) {
        return transactionTemplate.execute(status -> {
            Location location = locationMapper.fromCreateDto(dto);
            location.setState(LocationState.APPROVED);
            Location saved = locationRepository.save(location);
            return locationMapper.toFullDto(saved);
        });
    }

    @Override
    public LocationPrivateDtoOut addLocation(Long userId, LocationCreateDto dto) {
        // Внешний вызов — вне транзакции
        try {
            userClient.getUserById(userId);
        } catch (Exception e) {
            throw new NotFoundException("User", userId);
        }

        // Работа с БД — в отдельной короткой транзакции
        return transactionTemplate.execute(status -> {
            checkForDuplicate(dto.getName(), dto.getLatitude(), dto.getLongitude());

            Location location = locationMapper.fromCreateDto(dto);
            location.setCreatorId(userId);
            Location saved = locationRepository.save(location);
            return locationMapper.toPrivateDto(saved);
        });
    }

    @Override
    public LocationFullDtoOut update(Long id, LocationUpdateAdminDto dto) {
        log.debug("try update location by admin: {}", dto);

        return transactionTemplate.execute(status -> {
            Location location = locationRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Location", id));

            Optional.ofNullable(dto.getName()).ifPresent(location::setName);
            Optional.ofNullable(dto.getAddress()).ifPresent(location::setAddress);
            Optional.ofNullable(dto.getLatitude()).ifPresent(location::setLatitude);
            Optional.ofNullable(dto.getLongitude()).ifPresent(location::setLongitude);
            Optional.ofNullable(dto.getState()).ifPresent(
                    state -> changeLocationState(location, state));

            return locationMapper.toFullDto(location);
        });
    }

    @Override
    public LocationPrivateDtoOut update(Long id, Long userId, LocationUpdateUserDto dto) {
        return transactionTemplate.execute(status -> {
            Location location = locationRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Location", id));

            if (location.getState() != LocationState.PENDING) {
                throw new ConditionNotMetException("Cannot update published or rejected location");
            }

            if (location.getCreatorId() == null || !location.getCreatorId().equals(userId)) {
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

            return locationMapper.toPrivateDto(location);
        });
    }

    private void checkForDuplicate(String name, Double lat, Double lon) {
        log.debug("need check for duplicates");
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

        return locationMapper.toDto(location);
    }

    private void changeLocationState(Location location, LocationState state) {
        log.debug("changeLocationState id:{} state: {} -> {}", location.getId(), location.getState(), state);
        if (location.getState() == state) {
            return;
        }

        if (state == LocationState.PENDING || state == LocationState.AUTO_GENERATED) {
            throw new ConditionNotMetException(
                    String.format("Cannot change state from %s to %s", location.getState(), state));
        }
        location.setState(state);
    }

    private static String getDuplicateErrorMessage(@NotNull Location existing) {
        Long id = existing.getId();
        switch (existing.getState()) {
            case APPROVED -> {
                return String.format("Please use existing location (id=%d)", id);
            }
            case PENDING -> {
                return String.format("A request to create this location already exists (id=%d). Please wait for approval.", id);
            }
            case REJECTED -> {
                return "The request for creating this location was rejected earlier. Please contact admin.";
            }
        }
        return "";
    }

    @Override
    public Collection<LocationFullDtoOut> findAllByFilter(LocationAdminFilter filter) {
        Specification<Location> spec = buildSpecification(filter);
        List<Location> locations = locationRepository.findAll(spec, filter.getPageable()).getContent();
        return locations.stream()
                .map(locationMapper::toFullDto)
                .toList();
    }

    @Override
    public Collection<LocationPrivateDtoOut> findAllByFilter(Long userId, LocationPrivateFilter filter) {
        // внешний вызов — без транзакции
        try {
            userClient.getUserById(userId);
        } catch (Exception e) {
            throw new NotFoundException("User", userId);
        }

        Specification<Location> spec = buildSpecification(userId, filter);
        List<Location> locations = locationRepository.findAll(spec, filter.getPageable()).getContent();
        return locations.stream()
                .map(locationMapper::toPrivateDto)
                .toList();
    }

    @Override
    public Collection<LocationDtoOut> findAllByFilter(LocationPublicFilter filter) {
        Specification<Location> spec = buildSpecification(filter);
        List<Location> locations = locationRepository.findAll(spec, filter.getPageable()).getContent();
        return locations.stream()
                .map(locationMapper::toDto)
                .toList();
    }

    @Override
    public LocationFullDtoOut getByIdForAdmin(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location", id));
        return locationMapper.toFullDto(location);
    }

    @Override
    public void delete(Long id) {
        // 1. Внешний вызов — вне транзакции
        var events = eventClient.getEventsByLocation(id, 0, 1);
        if (events != null && !events.isEmpty()) {
            throw new ConditionNotMetException("There are events in this location");
        }

        // 2. Удаление в отдельной короткой транзакции
        transactionTemplate.executeWithoutResult(status -> locationRepository.deleteById(id));
    }

    @Override
    public void delete(Long id, Long userId) {
        // 1. Проверка прав и состояния — короткая транзакция
        transactionTemplate.executeWithoutResult(status -> {
            Location location = locationRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Location", id));

            if (location.getState() == LocationState.APPROVED) {
                throw new ConditionNotMetException("Cannot delete published location");
            }

            if (location.getCreatorId() == null || !location.getCreatorId().equals(userId)) {
                throw new NoAccessException("Only creator can delete this location");
            }
        });

        // 2. Внешний вызов — вне транзакции
        var events = eventClient.getEventsByLocation(id, 0, 1);
        if (events != null && !events.isEmpty()) {
            throw new ConditionNotMetException("There are events in this location");
        }

        // 3. Фактическое удаление — отдельная транзакция
        transactionTemplate.executeWithoutResult(status -> locationRepository.deleteById(id));
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

            return nearByAutoGenerated.orElseGet(
                    () -> createAutoGeneratedLocation(location.getLatitude(), location.getLongitude()));
        }

        throw new ConditionNotMetException("Invalid location");
    }

    private Location createAutoGeneratedLocation(Double lat, Double lon) {
        return transactionTemplate.execute(status -> {
            Location location = Location.builder()
                    .latitude(lat)
                    .longitude(lon)
                    .state(LocationState.AUTO_GENERATED)
                    .build();
            return locationRepository.save(location);
        });
    }

    private Specification<Location> buildSpecification(LocationAdminFilter filter) {
        return Stream.of(
                        optionalSpec(LocationSpecifications.withTextContains(filter.getText())),
                        optionalSpec(LocationSpecifications.withCreatorId(filter.getCreatorId())),
                        optionalSpec(LocationSpecifications.withCoordinates(filter.getZone())),
                        optionalSpec(LocationSpecifications.withState(filter.getState()))
                )
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }

    private Specification<Location> buildSpecification(Long userId, LocationPrivateFilter filter) {
        return Stream.of(
                        optionalSpec(LocationSpecifications.withCreatorId(userId)),
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

    @Override
    public LocationDtoOut getOrCreateAuto(LocationAutoRequest request) {
        LocationDto dto = new LocationDto();
        dto.setLatitude(request.getLatitude());
        dto.setLongitude(request.getLongitude());

        Location location = getOrCreateLocation(dto);

        return locationMapper.toDto(location);
    }
}
