package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.dto.LocationCreateDto;
import ru.practicum.ewm.dto.LocationPrivateDtoOut;
import ru.practicum.ewm.dto.LocationUpdateUserDto;
import ru.practicum.ewm.model.LocationPrivateFilter;
import ru.practicum.ewm.model.LocationState;
import ru.practicum.ewm.model.Zone;
import ru.practicum.ewm.service.LocationService;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/locations")
public class PrivateLocationController {

    private final LocationService locationService;

    /**
     * Создать локацию от имени пользователя.
     * Локация автоматически переводится в статус PENDING
     *
     * @return DTO созданной локации
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocationPrivateDtoOut create(@PathVariable @Min(1) Long userId,
                                        @RequestBody @Valid LocationCreateDto dto) {
        log.debug("request for adding location {} by user: {}", dto, userId);
        return locationService.addLocation(userId, dto);
    }

    /**
     * Редактировать существующую локацию от имени создателя (если локация в статусе PENDING)
     *
     * @return DTO обновленной локации
     */
    @PatchMapping("/{id}")
    public LocationPrivateDtoOut update(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long id,
            @RequestBody @Valid LocationUpdateUserDto dto) {
        log.debug("request for update location id: {} by user:{}", id, userId);
        return locationService.update(id, userId, dto);
    }

    /**
     * Получить список локаций, созданных текущим пользователем
     * Фильтрация по статусу, имени, координатам (с неким радиусом)
     *
     * @return список DTO локаций
     */
    @GetMapping
    public Collection<LocationPrivateDtoOut> getAll(
            @PathVariable @Min(1) Long userId,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) LocationState state,
            @RequestParam(required = false) @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,
            @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") Double lon,
            @RequestParam(defaultValue = "10.0") @DecimalMin("0.0") Double radius,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "10") Integer limit) {
        log.debug("request for search locations by user: {}", userId);
        LocationPrivateFilter filter = LocationPrivateFilter.builder()
                .text(text)
                .state(state)
                .offset(offset)
                .limit(limit)
                .build();

        if (lat != null && lon != null)
            filter.setZone(new Zone(lat, lon, radius));

        return locationService.findAllByFilter(userId, filter);
    }

    /**
     * Удалить существующую неопубликованную локацию от имени создателя.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Min(1) Long userId,
                       @PathVariable @Min(1) Long id) {
        log.debug("request for delete location id: {} by user:{}", id, userId);
        locationService.delete(id, userId);
    }
}
